package com.codeman.platform.tpl;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 模板版本留档:送审/审批轨迹(提交人/备注/审批人/意见/内容哈希)。 */
@Entity
@Table(name = "print_template_version")
public class PrintTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false, length = 40)
    private String templateCode;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 16)
    private String hash;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "submitted_by", length = 64)
    private String submittedBy;

    @Column(name = "submit_note", length = 256)
    private String submitNote;

    @Column(name = "reviewed_by", length = 64)
    private String reviewedBy;

    @Column(name = "review_note", length = 256)
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String v) { templateCode = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { version = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getHash() { return hash; }
    public void setHash(String v) { hash = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String v) { submittedBy = v; }
    public String getSubmitNote() { return submitNote; }
    public void setSubmitNote(String v) { submitNote = v; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String v) { reviewedBy = v; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String v) { reviewNote = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime v) { reviewedAt = v; }
}
