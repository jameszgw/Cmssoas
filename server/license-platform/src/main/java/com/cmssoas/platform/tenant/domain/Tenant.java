package com.cmssoas.platform.tenant.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "plan_code", nullable = false, length = 32)
    private String planCode;

    @Column(name = "plan_key", nullable = false, length = 32)
    private String planKey;

    @Column(nullable = false, length = 16)
    private String version;

    @Column(nullable = false, length = 64)
    private String isolation;

    @Column(nullable = false, length = 32)
    private String mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TenantStatus status;

    @Column(name = "admin_email", nullable = false, length = 128)
    private String adminEmail;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent;

    @Column(name = "expire_at")
    private LocalDate expireAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanKey() { return planKey; }
    public void setPlanKey(String planKey) { this.planKey = planKey; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getIsolation() { return isolation; }
    public void setIsolation(String isolation) { this.isolation = isolation; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    public LocalDate getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDate expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
