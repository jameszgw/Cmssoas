package com.codeman.platform.license.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "license")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_id", nullable = false, unique = true, length = 40)
    private String licenseId;

    @Column(name = "tenant_code", nullable = false, length = 32)
    private String tenantCode;

    @Column(nullable = false, length = 128)
    private String customer;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(nullable = false, length = 32)
    private String edition;

    @Column(nullable = false, length = 16)
    private String mode;

    @Column(nullable = false, length = 512)
    private String modules;

    @Column(nullable = false, length = 2000)
    private String features;

    @Column(name = "app_version_range", nullable = false, length = 64)
    private String appVersionRange;

    @Column(name = "not_before", nullable = false)
    private LocalDate notBefore;

    @Column(name = "not_after", nullable = false)
    private LocalDate notAfter;

    @Column(nullable = false)
    private int concurrency;

    @Column(nullable = false, length = 64)
    private String watermark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LicenseStatus status;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "expiry_reminded", nullable = false)
    private boolean expiryReminded;

    @Column(name = "claims_json", nullable = false, columnDefinition = "text")
    private String claimsJson;

    @Column(nullable = false, length = 256)
    private String signature;

    @Column(nullable = false, columnDefinition = "text")
    private String lic;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String v) { this.licenseId = v; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { this.customer = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public String getEdition() { return edition; }
    public void setEdition(String v) { this.edition = v; }
    public String getMode() { return mode; }
    public void setMode(String v) { this.mode = v; }
    public String getModules() { return modules; }
    public void setModules(String v) { this.modules = v; }
    public String getFeatures() { return features; }
    public void setFeatures(String v) { this.features = v; }
    public String getAppVersionRange() { return appVersionRange; }
    public void setAppVersionRange(String v) { this.appVersionRange = v; }
    public LocalDate getNotBefore() { return notBefore; }
    public void setNotBefore(LocalDate v) { this.notBefore = v; }
    public LocalDate getNotAfter() { return notAfter; }
    public void setNotAfter(LocalDate v) { this.notAfter = v; }
    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int v) { this.concurrency = v; }
    public String getWatermark() { return watermark; }
    public void setWatermark(String v) { this.watermark = v; }
    public LicenseStatus getStatus() { return status; }
    public void setStatus(LicenseStatus v) { this.status = v; }
    public int getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(int v) { this.currentVersion = v; }
    public String getClaimsJson() { return claimsJson; }
    public void setClaimsJson(String v) { this.claimsJson = v; }
    public String getSignature() { return signature; }
    public void setSignature(String v) { this.signature = v; }
    public String getLic() { return lic; }
    public void setLic(String v) { this.lic = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public boolean isExpiryReminded() { return expiryReminded; }
    public void setExpiryReminded(boolean v) { this.expiryReminded = v; }
}
