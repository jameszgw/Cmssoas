package com.codeman.platform.tenant.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 租户用户（此处用于初始超级管理员）。 */
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String email;

    /** 开通时为空，激活时由管理员自设（BCrypt）。 */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 32)
    private String role;

    /** PENDING_ACTIVATION / ACTIVE / DISABLED */
    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "must_change_pwd", nullable = false)
    private boolean mustChangePwd;

    @Column(name = "mfa_bound", nullable = false)
    private boolean mfaBound;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isMustChangePwd() { return mustChangePwd; }
    public void setMustChangePwd(boolean mustChangePwd) { this.mustChangePwd = mustChangePwd; }
    public boolean isMfaBound() { return mfaBound; }
    public void setMfaBound(boolean mfaBound) { this.mfaBound = mfaBound; }
    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
