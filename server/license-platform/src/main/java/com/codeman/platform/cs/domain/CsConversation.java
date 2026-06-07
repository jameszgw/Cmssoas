package com.codeman.platform.cs.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 智能客服会话。一个会话聚合多轮消息；可转人工(ESCALATED)或关闭(CLOSED)。 */
@Entity
@Table(name = "cs_conversation")
public class CsConversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_code", length = 32) private String tenantCode;
    @Column(name = "user_ref", nullable = false, length = 128) private String userRef;
    @Column(length = 160) private String title;
    @Column(nullable = false, length = 16) private String status;   // OPEN/ESCALATED/CLOSED
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getUserRef() { return userRef; }
    public void setUserRef(String v) { this.userRef = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
