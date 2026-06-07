package com.cmssoas.platform.rbac.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ops_user")
public class OpsUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 64) private String username;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "role_id", nullable = false) private Long roleId;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "must_change_pwd", nullable = false) private boolean mustChangePwd;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long v) { this.roleId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isMustChangePwd() { return mustChangePwd; }
    public void setMustChangePwd(boolean v) { this.mustChangePwd = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
