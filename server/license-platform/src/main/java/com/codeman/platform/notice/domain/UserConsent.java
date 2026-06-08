package com.codeman.platform.notice.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 用户授权(同意管理)：记录“谁、何时、对哪个协议版本、以何方式”同意/撤回，可查询、可导出、可撤回。 */
@Entity
@Table(name = "user_consent")
public class UserConsent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_code", length = 32) private String tenantCode;
    @Column(nullable = false, length = 128) private String subject;
    @Column(name = "notice_id", nullable = false) private Long noticeId;
    @Column(name = "notice_type", nullable = false, length = 16) private String noticeType;
    @Column(nullable = false) private int version;
    @Column(nullable = false, length = 16) private String action;     // GRANTED/REVOKED
    @Column(nullable = false, length = 16) private String channel;    // WEB/EMAIL/API
    @Column(length = 64) private String ip;
    @Column(name = "user_agent", length = 256) private String userAgent;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long v) { this.noticeId = v; }
    public String getNoticeType() { return noticeType; }
    public void setNoticeType(String v) { this.noticeType = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
