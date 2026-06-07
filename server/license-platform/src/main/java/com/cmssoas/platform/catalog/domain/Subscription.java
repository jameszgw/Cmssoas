package com.cmssoas.platform.catalog.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_code", nullable = false, length = 32) private String tenantCode;
    @Column(nullable = false, length = 128) private String customer;
    @Column(name = "plan_code", nullable = false, length = 32) private String planCode;
    @Column(nullable = false) private int qty;
    @Column(name = "start_at", nullable = false) private LocalDate startAt;
    @Column(name = "end_at", nullable = false) private LocalDate endAt;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "license_id", length = 40) private String licenseId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { this.customer = v; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String v) { this.planCode = v; }
    public int getQty() { return qty; }
    public void setQty(int v) { this.qty = v; }
    public LocalDate getStartAt() { return startAt; }
    public void setStartAt(LocalDate v) { this.startAt = v; }
    public LocalDate getEndAt() { return endAt; }
    public void setEndAt(LocalDate v) { this.endAt = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String v) { this.licenseId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
