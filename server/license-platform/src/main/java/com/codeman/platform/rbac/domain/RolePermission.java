package com.codeman.platform.rbac.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "ops_role_permission")
public class RolePermission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false) private Long roleId;
    @Column(name = "perm_code", nullable = false, length = 48) private String permCode;
    @Column(nullable = false, length = 16) private String mode;

    public RolePermission() {}
    public RolePermission(Long roleId, String permCode, String mode) {
        this.roleId = roleId; this.permCode = permCode; this.mode = mode;
    }
    public Long getId() { return id; }
    public Long getRoleId() { return roleId; }
    public String getPermCode() { return permCode; }
    public String getMode() { return mode; }
}
