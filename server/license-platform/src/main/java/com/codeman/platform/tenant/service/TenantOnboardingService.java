package com.codeman.platform.tenant.service;

import com.codeman.platform.config.AppProperties;
import com.codeman.platform.mail.OnboardingMailService;
import com.codeman.platform.mail.Totp;
import com.codeman.platform.tenant.domain.*;
import com.codeman.platform.tenant.dto.OnboardTenantRequest;
import com.codeman.platform.tenant.dto.OnboardTenantResponse;
import com.codeman.platform.tenant.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 租户开通编排（Saga 风格的状态机推进）：
 *   建租户 → 资源/隔离编排 → DB 初始化(Flyway 占位) → 种子数据 → 创建超管 →
 *   生成一次性激活令牌 → 发送开通邮件 → 置为 ACTIVE。
 * 任一 DB 步骤异常则整体回滚（@Transactional），邮件失败不回滚（记录 emailSent=false，可重发）。
 */
@Service
public class TenantOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(TenantOnboardingService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepo;
    private final SysUserRepository userRepo;
    private final ActivationTokenRepository tokenRepo;
    private final AuditLogRepository auditRepo;
    private final OnboardingMailService mailService;
    private final AppProperties props;
    private final Totp totp;
    private final TenantSchemaService schemaService;

    public TenantOnboardingService(TenantRepository tenantRepo, SysUserRepository userRepo,
                                   ActivationTokenRepository tokenRepo, AuditLogRepository auditRepo,
                                   OnboardingMailService mailService, AppProperties props, Totp totp,
                                   TenantSchemaService schemaService) {
        this.tenantRepo = tenantRepo;
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.auditRepo = auditRepo;
        this.mailService = mailService;
        this.props = props;
        this.totp = totp;
        this.schemaService = schemaService;
    }

    @Transactional
    public OnboardTenantResponse onboard(OnboardTenantRequest req) {
        // —— 步骤 1：创建租户记录（INITIALIZING）
        Tenant tenant = new Tenant();
        tenant.setCode(nextCode());
        tenant.setName(req.name().trim());
        tenant.setPlanCode(planCode(req.plan()));
        tenant.setPlanKey(planKey(req.plan()));
        tenant.setVersion(req.version() == null || req.version().isBlank() ? "v2.4.0" : req.version());
        tenant.setIsolation(orDefault(req.isolation(), "共享库 · 行级隔离 (RLS)"));
        tenant.setMode(orDefault(req.mode(), "混合 (HYBRID)"));
        tenant.setAdminEmail(req.email().trim());
        tenant.setStatus(TenantStatus.INITIALIZING);
        tenant.setEmailSent(false);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant = tenantRepo.save(tenant);
        audit(tenant.getId(), "TENANT_CREATED", "租户记录已创建：" + tenant.getCode());

        // —— 步骤 2：资源/隔离编排（真实创建独立 Schema，物理隔离租户数据）
        schemaService.createSchema(tenant.getCode());
        audit(tenant.getId(), "PROVISION_ISOLATION",
                "隔离级别：" + tenant.getIsolation() + "；Schema=" + schemaService.schemaOf(tenant.getCode()));

        // —— 步骤 3：DB 初始化（在租户 Schema 内建租户级业务表）
        schemaService.migrate(tenant.getCode());
        audit(tenant.getId(), "DB_MIGRATE", "租户 Schema 内建表完成");

        // —— 步骤 4：种子数据（写入默认角色与配置到租户 Schema）
        schemaService.seed(tenant.getCode(), tenant.getPlanCode());
        audit(tenant.getId(), "SEED_DATA", "写入默认角色与配置（租户 Schema）");

        // —— 步骤 5：创建初始超级管理员（密码留空，待激活时自设）
        SysUser admin = new SysUser();
        admin.setTenantId(tenant.getId());
        admin.setUsername("admin");
        admin.setEmail(tenant.getAdminEmail());
        admin.setRole("SUPER_ADMIN");
        admin.setStatus("PENDING_ACTIVATION");
        admin.setMustChangePwd(true);
        admin.setMfaBound(false);
        admin.setMfaSecret(totp.generateSecret());
        admin.setCreatedAt(LocalDateTime.now());
        admin = userRepo.save(admin);
        audit(tenant.getId(), "SUPER_ADMIN_CREATED", "超管已创建（待激活）：" + admin.getEmail());

        // —— 步骤 6：生成一次性激活令牌
        ActivationToken token = new ActivationToken();
        token.setToken(randomToken());
        token.setTenantId(tenant.getId());
        token.setUserId(admin.getId());
        token.setExpiresAt(LocalDateTime.now().plusHours(props.getActivation().getTtlHours()));
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        tokenRepo.save(token);

        // —— 步骤 7：开通邮件入发件箱(outbox)，与本事务原子提交，异步投递
        boolean queued = mailService.enqueueOnboarding(tenant, token.getToken());
        tenant.setEmailSent(queued);

        // —— 步骤 8：置为 ACTIVE
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setExpireAt(LocalDate.now().plusYears(1));
        tenantRepo.save(tenant);
        audit(tenant.getId(), "ONBOARD_DONE", "开通完成，开通邮件已入发件箱");

        log.info("[onboard] 租户 {} 开通完成，邮件已入队", tenant.getCode());
        return new OnboardTenantResponse(tenant.getCode(), queued, tenant.getAdminEmail());
    }

    private void audit(Long tenantId, String action, String detail) {
        auditRepo.save(AuditLog.of(tenantId, "operator", action, detail));
    }

    private String nextCode() {
        long base = 100482 + tenantRepo.count();
        String code;
        do {
            base++;
            code = "T-" + base;
        } while (tenantRepo.existsByCode(code));
        return code;
    }

    private String randomToken() {
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String orDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private static String planCode(String plan) {
        if (plan == null) return "ENTERPRISE";
        return switch (plan) {
            case "旗舰版", "Flagship" -> "FLAGSHIP";
            case "专业版", "Professional" -> "PROFESSIONAL";
            case "基础版", "Basic" -> "BASIC";
            default -> "ENTERPRISE";
        };
    }

    private static String planKey(String plan) {
        return switch (planCode(plan)) {
            case "FLAGSHIP" -> "plan.flag";
            case "PROFESSIONAL" -> "plan.pro";
            case "BASIC" -> "plan.basic";
            default -> "plan.ent";
        };
    }
}
