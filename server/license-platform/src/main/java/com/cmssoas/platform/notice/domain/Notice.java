package com.cmssoas.platform.notice.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 用户须知/服务条款/隐私政策/公告。版本化：每次发布生成新版本，旧版归档可追溯。 */
@Entity
@Table(name = "notice")
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 16) private String type;       // TERMS/PRIVACY/NOTICE/ANNOUNCEMENT
    @Column(nullable = false, length = 160) private String title;
    @Column(name = "content_html", nullable = false, columnDefinition = "text") private String contentHtml;
    @Column(nullable = false) private int version = 1;
    @Column(nullable = false, length = 16) private String status;     // DRAFT/PUBLISHED/ARCHIVED
    @Column(name = "force_ack", nullable = false) private boolean forceAck = false;
    @Column(name = "effective_at") private LocalDateTime effectiveAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String v) { this.contentHtml = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isForceAck() { return forceAck; }
    public void setForceAck(boolean v) { this.forceAck = v; }
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDateTime v) { this.effectiveAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
