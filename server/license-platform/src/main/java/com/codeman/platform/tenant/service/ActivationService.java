package com.codeman.platform.tenant.service;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.mail.Totp;
import com.codeman.platform.tenant.domain.ActivationToken;
import com.codeman.platform.tenant.domain.AuditLog;
import com.codeman.platform.tenant.domain.SysUser;
import com.codeman.platform.tenant.domain.Tenant;
import com.codeman.platform.tenant.dto.ActivationInfo;
import com.codeman.platform.tenant.repo.ActivationTokenRepository;
import com.codeman.platform.tenant.repo.AuditLogRepository;
import com.codeman.platform.tenant.repo.SysUserRepository;
import com.codeman.platform.tenant.repo.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivationService {

    private final ActivationTokenRepository tokenRepo;
    private final SysUserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final AuditLogRepository auditRepo;
    private final PasswordEncoder passwordEncoder;
    private final Totp totp;

    public ActivationService(ActivationTokenRepository tokenRepo, SysUserRepository userRepo,
                             TenantRepository tenantRepo, AuditLogRepository auditRepo,
                             PasswordEncoder passwordEncoder, Totp totp) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.auditRepo = auditRepo;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
    }

    /** 激活页信息（不抛异常，返回 valid 标记供前端友好提示）。 */
    @Transactional(readOnly = true)
    public ActivationInfo info(String token) {
        ActivationToken at = tokenRepo.findByToken(token).orElse(null);
        if (at == null) return invalid("激活链接无效");
        if (at.isUsed()) return invalid("激活链接已被使用");
        if (at.isExpired()) return invalid("激活链接已过期");
        Tenant t = tenantRepo.findById(at.getTenantId()).orElse(null);
        if (t == null) return invalid("租户不存在");
        SysUser admin = userRepo.findById(at.getUserId()).orElse(null);
        String secret = admin == null ? null : admin.getMfaSecret();
        String uri = (admin == null || secret == null) ? null : totp.otpauthUri(secret, admin.getEmail());
        return new ActivationInfo(t.getName(), t.getCode(), t.getAdminEmail(), t.getVersion(),
                uri, secret, true, "OK");
    }

    /** 激活并设置密码；提供 mfaCode 则校验并绑定 MFA。 */
    @Transactional
    public void activate(String token, String rawPassword, String mfaCode) {
        ActivationToken at = tokenRepo.findByToken(token)
                .orElseThrow(() -> ApiException.notFound("激活链接无效"));
        if (at.isUsed()) throw ApiException.gone("激活链接已被使用");
        if (at.isExpired()) throw ApiException.gone("激活链接已过期");

        SysUser admin = userRepo.findById(at.getUserId())
                .orElseThrow(() -> ApiException.notFound("管理员账户不存在"));

        boolean mfaBound = false;
        if (mfaCode != null && !mfaCode.isBlank()) {
            if (!totp.verify(admin.getMfaSecret(), mfaCode)) {
                throw ApiException.badRequest("MFA 验证码不正确，请重试");
            }
            mfaBound = true;
        }

        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setStatus("ACTIVE");
        admin.setMustChangePwd(false);
        admin.setMfaBound(mfaBound);
        userRepo.save(admin);

        at.setUsed(true);
        tokenRepo.save(at);

        auditRepo.save(AuditLog.of(at.getTenantId(), admin.getEmail(),
                "ADMIN_ACTIVATED", "超管已激活并设置密码；MFA=" + (mfaBound ? "已绑定" : "未绑定")));
    }

    private ActivationInfo invalid(String msg) {
        return new ActivationInfo(null, null, null, null, null, null, false, msg);
    }
}
