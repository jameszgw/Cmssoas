package com.codeman.platform.tenant.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 512)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AuditLog of(Long tenantId, String actor, String action, String detail) {
        AuditLog a = new AuditLog();
        a.tenantId = tenantId;
        a.actor = actor;
        a.action = action;
        a.detail = detail;
        a.createdAt = LocalDateTime.now();
        return a;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
