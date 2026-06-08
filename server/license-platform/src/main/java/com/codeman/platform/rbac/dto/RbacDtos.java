package com.codeman.platform.rbac.dto;

import com.codeman.platform.rbac.domain.OpsRole;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public final class RbacDtos {
    private RbacDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record PermItem(String code, String mode) {}

    public record AuthResult(String token, String username, String role, String roleName,
                             boolean mustChangePwd, List<PermItem> permissions) {}

    /** 权限树节点（定义；含子节点）。 */
    public record PermNode(String code, String name, String type, List<PermNode> children) {}

    public record RoleView(Long id, String code, String name, String description) {
        public static RoleView from(OpsRole r) {
            return new RoleView(r.getId(), r.getCode(), r.getName(), r.getDescription());
        }
    }

    /** 角色详情：权限定义树 + 该角色每个节点的多态 mode。 */
    public record RoleDetail(RoleView role, Map<String, String> modes) {}
}
