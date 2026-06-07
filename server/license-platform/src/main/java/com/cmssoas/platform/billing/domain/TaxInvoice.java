package com.cmssoas.platform.billing.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 正规税务发票(电子发票)。对已收款账单申请开具,经渠道开票后回填发票代码/号码/PDF。 */
@Entity
@Table(name = "tax_invoice")
public class TaxInvoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "invoice_id", nullable = false) private Long invoiceId;
    @Column(nullable = false, length = 160) private String title;
    @Column(name = "tax_no", length = 32) private String taxNo;
    @Column(nullable = false, length = 16) private String type;     // NORMAL/SPECIAL
    @Column(length = 128) private String email;
    @Column(nullable = false) private int amount;
    @Column(nullable = false, length = 16) private String status;   // ISSUING/ISSUED/FAILED
    @Column(name = "invoice_code", length = 32) private String invoiceCode;
    @Column(name = "invoice_serial", length = 32) private String invoiceSerial;
    @Column(name = "pdf_url", length = 512) private String pdfUrl;
    @Column(length = 16) private String provider;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "issued_at") private LocalDateTime issuedAt;

    public Long getId() { return id; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long v) { this.invoiceId = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getTaxNo() { return taxNo; }
    public void setTaxNo(String v) { this.taxNo = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String v) { this.invoiceCode = v; }
    public String getInvoiceSerial() { return invoiceSerial; }
    public void setInvoiceSerial(String v) { this.invoiceSerial = v; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String v) { this.pdfUrl = v; }
    public String getProvider() { return provider; }
    public void setProvider(String v) { this.provider = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime v) { this.issuedAt = v; }
}
