package com.cmssoas.platform.tenant.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 邮件发件箱（事务 outbox 模式）。 */
@Entity
@Table(name = "email_outbox")
public class EmailOutbox {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id") private Long tenantId;
    @Column(name = "to_addr", nullable = false, length = 128) private String toAddr;
    @Column(nullable = false, length = 256) private String subject;
    @Column(name = "body_html", nullable = false, columnDefinition = "text") private String bodyHtml;
    @Column(nullable = false, length = 16) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "next_attempt_at", nullable = false) private LocalDateTime nextAttemptAt;
    @Column(name = "last_error", length = 512) private String lastError;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long v) { this.tenantId = v; }
    public String getToAddr() { return toAddr; }
    public void setToAddr(String v) { this.toAddr = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String v) { this.bodyHtml = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int v) { this.attempts = v; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int v) { this.maxAttempts = v; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime v) { this.nextAttemptAt = v; }
    public String getLastError() { return lastError; }
    public void setLastError(String v) { this.lastError = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime v) { this.sentAt = v; }
}
