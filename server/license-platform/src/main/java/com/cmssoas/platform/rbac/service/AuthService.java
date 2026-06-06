package com.cmssoas.platform.rbac.service;

import com.cmssoas.platform.common.ApiException;
import com.cmssoas.platform.rbac.domain.OpsRole;
import com.cmssoas.platform.rbac.domain.OpsUser;
import com.cmssoas.platform.rbac.dto.RbacDtos.*;
import com.cmssoas.platform.rbac.repo.OpsRoleRepository;
import com.cmssoas.platform.rbac.repo.OpsUserRepository;
import com.cmssoas.platform.rbac.repo.RolePermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final OpsUserRepository userRepo;
    private final OpsRoleRepository roleRepo;
    private final RolePermissionRepository rolePermRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(OpsUserRepository userRepo, OpsRoleRepository roleRepo,
                       RolePermissionRepository rolePermRepo, PasswordEncoder encoder, JwtService jwt) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.rolePermRepo = rolePermRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResult login(LoginRequest req) {
        OpsUser u = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!"ACTIVE".equals(u.getStatus()) || !encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        OpsRole role = roleRepo.findById(u.getRoleId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "角色不存在"));
        String token = jwt.generate(u.getId(), u.getUsername(), role.getCode());
        return new AuthResult(token, u.getUsername(), role.getCode(), role.getName(),
                u.isMustChangePwd(), effectivePerms(u.getRoleId()));
    }

    public AuthResult me(Long uid) {
        OpsUser u = userRepo.findById(uid)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        OpsRole role = roleRepo.findById(u.getRoleId()).orElseThrow();
        return new AuthResult(null, u.getUsername(), role.getCode(), role.getName(),
                u.isMustChangePwd(), effectivePerms(u.getRoleId()));
    }

    private List<PermItem> effectivePerms(Long roleId) {
        return rolePermRepo.findByRoleId(roleId).stream()
                .filter(rp -> !"NONE".equals(rp.getMode()))
                .map(rp -> new PermItem(rp.getPermCode(), rp.getMode()))
                .toList();
    }
}
