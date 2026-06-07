package com.cmssoas.platform.billing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 支付单。由待支付账单(Invoice PENDING)发起,生成扫码/跳转支付;
 * 渠道异步回调或(沙箱)模拟确认后置 PAID,并联动账单收款。
 */
@Entity
@Table(name = "payment")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "payment_no", nullable = false, length = 40) private String paymentNo;
    @Column(name = "invoice_id", nullable = false) private Long invoiceId;
    @Column(name = "tenant_code", length = 32) private String tenantCode;
    @Column(length = 128) private String customer;
    @Column(nullable = false) private int amount;
    @Column(nullable = false, length = 8) private String currency;
    @Column(nullable = false, length = 16) private String channel;   // MOCK/WECHATPAY/ALIPAY/STRIPE
    @Column(nullable = false, length = 16) private String status;    // CREATED/PAID/FAILED/CLOSED
    @Column(name = "qr_content", length = 512) private String qrContent;
    @Column(name = "pay_url", length = 512) private String payUrl;
    @Column(name = "provider_txn_id", length = 64) private String providerTxnId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "paid_at") private LocalDateTime paidAt;

    public Long getId() { return id; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String v) { this.paymentNo = v; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long v) { this.invoiceId = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { this.customer = v; }
    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getQrContent() { return qrContent; }
    public void setQrContent(String v) { this.qrContent = v; }
    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String v) { this.payUrl = v; }
    public String getProviderTxnId() { return providerTxnId; }
    public void setProviderTxnId(String v) { this.providerTxnId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }
}
