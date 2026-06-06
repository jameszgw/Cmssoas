package com.cmssoas.platform.rbac.service;

import com.cmssoas.platform.rbac.domain.OpsRole;
import com.cmssoas.platform.rbac.repo.OpsRoleRepository;
import com.cmssoas.platform.rbac.repo.RolePermissionRepository;
import org.springframework.stereotype.Service;

@Service
public class PermissionChecker {

    private final OpsRoleRepository roleRepo;
    private final RolePermissionRepository rolePermRepo;

    public PermissionChecker(OpsRoleRepository roleRepo, RolePermissionRepository rolePermRepo) {
        this.roleRepo = roleRepo;
        this.rolePermRepo = rolePermRepo;
    }

    /** 当前用户是否拥有该权限点（mode 非 NONE）；SUPER_ADMIN 始终放行。 */
    public boolean granted(String permCode) {
        CurrentUser.Ctx ctx = CurrentUser.get();
        if (ctx == null) return false;
        if ("SUPER_ADMIN".equals(ctx.role())) return true;
        OpsRole role = roleRepo.findByCode(ctx.role()).orElse(null);
        if (role == null) return false;
        return rolePermRepo.findByRoleId(role.getId()).stream()
                .anyMatch(rp -> rp.getPermCode().equals(permCode) && !"NONE".equals(rp.getMode()));
    }
}
