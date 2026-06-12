package com.codeman.platform.tpl;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 模板资产主表:生效内容(content/currentVersion) + 工作副本(draftContent)。
 * status: DRAFT(未生效过)/PENDING(送审中,只读)/APPROVED(有生效版)/DISABLED(下架)。
 */
@Entity
@Table(name = "print_template")
public class PrintTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "tenant_code", length = 32)
    private String tenantCode;

    @Column(length = 256)
    private String tags;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "draft_content", columnDefinition = "text")
    private String draftContent;

    @Column(name = "use_count", nullable = false)
    private int useCount;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { productCode = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { tenantCode = v; }
    public String getTags() { return tags; }
    public void setTags(String v) { tags = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public int getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(int v) { currentVersion = v; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getDraftContent() { return draftContent; }
    public void setDraftContent(String v) { draftContent = v; }
    public int getUseCount() { return useCount; }
    public void setUseCount(int v) { useCount = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
