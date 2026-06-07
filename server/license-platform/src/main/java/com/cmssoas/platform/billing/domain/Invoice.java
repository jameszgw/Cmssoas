package com.cmssoas.platform.billing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 账单/发票。订阅或变更时自动生成 PENDING；收款→PAID；开票→INVOICED。 */
@Entity
@Table(name = "invoice")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "invoice_no", length = 40) private String invoiceNo;
    @Column(name = "tenant_code", nullable = false, length = 32) private String tenantCode;
    @Column(nullable = false, length = 128) private String customer;
    @Column(name = "subscription_id") private Long subscriptionId;
    @Column(name = "plan_code", length = 32) private String planCode;
    @Column(nullable = false, length = 16) private String type;
    @Column(nullable = false) private int amount;
    @Column(nullable = false, length = 8) private String currency;
    @Column(length = 40) private String period;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "invoiced_at") private LocalDateTime invoicedAt;

    public Long getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String v) { this.invoiceNo = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { this.customer = v; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long v) { this.subscriptionId = v; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String v) { this.planCode = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getPeriod() { return period; }
    public void setPeriod(String v) { this.period = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }
    public LocalDateTime getInvoicedAt() { return invoicedAt; }
    public void setInvoicedAt(LocalDateTime v) { this.invoicedAt = v; }
}
