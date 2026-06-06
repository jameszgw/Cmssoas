package com.cmssoas.platform.rbac.service;

import com.cmssoas.platform.common.ApiException;
import com.cmssoas.platform.rbac.domain.OpsRole;
import com.cmssoas.platform.rbac.domain.Permission;
import com.cmssoas.platform.rbac.domain.RolePermission;
import com.cmssoas.platform.rbac.dto.RbacDtos.*;
import com.cmssoas.platform.rbac.repo.OpsRoleRepository;
import com.cmssoas.platform.rbac.repo.PermissionRepository;
import com.cmssoas.platform.rbac.repo.RolePermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RbacService {

    private final PermissionRepository permRepo;
    private final OpsRoleRepository roleRepo;
    private final RolePermissionRepository rolePermRepo;

    public RbacService(PermissionRepository permRepo, OpsRoleRepository roleRepo,
                       RolePermissionRepository rolePermRepo) {
        this.permRepo = permRepo;
        this.roleRepo = roleRepo;
        this.rolePermRepo = rolePermRepo;
    }

    /** 权限定义树（按 parentCode 组装；先建全部节点再链接，避免顺序依赖）。 */
    public List<PermNode> permissionTree() {
        List<Permission> all = permRepo.findAllByOrderBySortAsc();
        Map<String, PermNode> map = new LinkedHashMap<>();
        for (Permission p : all) map.put(p.getCode(), new PermNode(p.getCode(), p.getName(), p.getType(), new ArrayList<>()));
        List<PermNode> roots = new ArrayList<>();
        for (Permission p : all) {
            PermNode node = map.get(p.getCode());
            PermNode parent = p.getParentCode() == null ? null : map.get(p.getParentCode());
            if (parent != null) parent.children().add(node);
            else roots.add(node);
        }
        return roots;
    }

    public List<RoleView> roles() {
        return roleRepo.findAll().stream().map(RoleView::from).toList();
    }

    public RoleDetail roleDetail(Long roleId) {
        OpsRole role = roleRepo.findById(roleId)
                .orElseThrow(() -> ApiException.notFound("角色不存在"));
        Map<String, String> modes = new HashMap<>();
        for (RolePermission rp : rolePermRepo.findByRoleId(roleId)) modes.put(rp.getPermCode(), rp.getMode());
        return new RoleDetail(RoleView.from(role), modes);
    }

    @Transactional
    public void setRolePermissions(Long roleId, List<PermItem> items) {
        if (!CurrentUser.isSuperAdmin()) throw new ApiException(HttpStatus.FORBIDDEN, "仅超级管理员可修改权限");
        roleRepo.findById(roleId).orElseThrow(() -> ApiException.notFound("角色不存在"));
        rolePermRepo.deleteByRoleId(roleId);
        for (PermItem it : items) {
            if (it.mode() == null || "NONE".equals(it.mode())) continue;
            rolePermRepo.save(new RolePermission(roleId, it.code(), it.mode()));
        }
    }
}
