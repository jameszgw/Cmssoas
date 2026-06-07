package com.codeman.platform.tenant.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_log")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "to_addr", nullable = false, length = 128)
    private String toAddr;

    @Column(nullable = false, length = 256)
    private String subject;

    /** SENT / SPOOLED / FAILED */
    @Column(nullable = false, length = 24)
    private String status;

    @Column(length = 512)
    private String error;

    @Column(name = "rendered_path", length = 256)
    private String renderedPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getToAddr() { return toAddr; }
    public void setToAddr(String toAddr) { this.toAddr = toAddr; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getRenderedPath() { return renderedPath; }
    public void setRenderedPath(String renderedPath) { this.renderedPath = renderedPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
