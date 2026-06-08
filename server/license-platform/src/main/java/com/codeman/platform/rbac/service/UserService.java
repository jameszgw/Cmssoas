package com.codeman.platform.rbac.service;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.rbac.domain.OpsRole;
import com.codeman.platform.rbac.domain.OpsUser;
import com.codeman.platform.rbac.dto.UserDtos.*;
import com.codeman.platform.rbac.repo.OpsRoleRepository;
import com.codeman.platform.rbac.repo.OpsUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    public static final String DEFAULT_PWD = "Codeman@123";

    private final OpsUserRepository userRepo;
    private final OpsRoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final com.codeman.platform.common.AuditWriter audit;

    public UserService(OpsUserRepository userRepo, OpsRoleRepository roleRepo, PasswordEncoder encoder,
                       com.codeman.platform.common.AuditWriter audit) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder = encoder;
        this.audit = audit;
    }

    public List<UserView> list() {
        Map<Long, OpsRole> roles = roleRepo.findAll().stream()
                .collect(Collectors.toMap(OpsRole::getId, Function.identity()));
        return userRepo.findAll().stream().map(u -> {
            OpsRole r = roles.get(u.getRoleId());
            return new UserView(u.getId(), u.getUsername(),
                    r == null ? "" : r.getCode(), r == null ? "" : r.getName(),
                    u.getStatus(), u.isMustChangePwd(), u.getCreatedAt().toString());
        }).toList();
    }

    public UserView create(CreateUserRequest req) {
        if (userRepo.findByUsername(req.username()).isPresent())
            throw ApiException.badRequest("用户名已存在");
        OpsRole role = roleRepo.findById(req.roleId())
                .orElseThrow(() -> ApiException.badRequest("角色不存在"));
        String pwd = (req.password() == null || req.password().isBlank()) ? DEFAULT_PWD : req.password();
        OpsUser u = new OpsUser();
        u.setUsername(req.username().trim());
        u.setPasswordHash(encoder.encode(pwd));
        u.setRoleId(role.getId());
        u.setStatus("ACTIVE");
        u.setMustChangePwd(true);   // 首登强制改密
        u.setCreatedAt(LocalDateTime.now());
        userRepo.save(u);
        audit.log(null, "USER_CREATE", u.getUsername() + " · 角色=" + role.getCode());
        return new UserView(u.getId(), u.getUsername(), role.getCode(), role.getName(),
                u.getStatus(), true, u.getCreatedAt().toString());
    }

    public void resetPassword(Long id, ResetPwdRequest req) {
        OpsUser u = userRepo.findById(id).orElseThrow(() -> ApiException.notFound("用户不存在"));
        String pwd = (req.password() == null || req.password().isBlank()) ? DEFAULT_PWD : req.password();
        u.setPasswordHash(encoder.encode(pwd));
        u.setMustChangePwd(true);
        userRepo.save(u);
        audit.log(null, "USER_RESET_PWD", u.getUsername());
    }

    public void toggle(Long id) {
        OpsUser u = userRepo.findById(id).orElseThrow(() -> ApiException.notFound("用户不存在"));
        if ("admin".equals(u.getUsername())) throw ApiException.badRequest("不能停用内置管理员");
        u.setStatus("ACTIVE".equals(u.getStatus()) ? "DISABLED" : "ACTIVE");
        userRepo.save(u);
        audit.log(null, "USER_TOGGLE", u.getUsername() + " -> " + u.getStatus());
    }

    public void changePassword(Long uid, ChangePwdRequest req) {
        OpsUser u = userRepo.findById(uid).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        if (!encoder.matches(req.oldPassword(), u.getPasswordHash()))
            throw ApiException.badRequest("原密码不正确");
        u.setPasswordHash(encoder.encode(req.newPassword()));
        u.setMustChangePwd(false);
        userRepo.save(u);
    }
}
