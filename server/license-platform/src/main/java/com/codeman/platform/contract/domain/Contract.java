package com.codeman.platform.contract.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 合同。生命周期：DRAFT(草稿) → SENT/SIGNING(已发起，待各方签署) → SIGNED(全部签署，存证归档) / VOID(作废)。
 * 发起签署时对内容做快照与 SHA-256 哈希(content_hash)防篡改。
 */
@Entity
@Table(name = "contract")
public class Contract {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "contract_no", length = 40) private String contractNo;
    @Column(name = "tenant_code", length = 32) private String tenantCode;
    @Column(nullable = false, length = 128) private String customer;
    @Column(name = "subscription_id") private Long subscriptionId;
    @Column(name = "plan_code", length = 32) private String planCode;
    @Column(name = "template_id") private Long templateId;
    @Column(nullable = false, length = 160) private String title;
    @Column(name = "content_html", nullable = false, columnDefinition = "text") private String contentHtml;
    @Column(nullable = false) private int amount = 0;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "content_hash", length = 80) private String contentHash;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "sent_at") private LocalDateTime sentAt;
    @Column(name = "signed_at") private LocalDateTime signedAt;

    public Long getId() { return id; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String v) { this.contractNo = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { this.customer = v; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long v) { this.subscriptionId = v; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String v) { this.planCode = v; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long v) { this.templateId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String v) { this.contentHtml = v; }
    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String v) { this.contentHash = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime v) { this.sentAt = v; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime v) { this.signedAt = v; }
}
