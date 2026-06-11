package com.codeman.platform.license.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** License 每次变更的不可变快照，用于版本历史与追溯。 */
@Entity
@Table(name = "license_history")
public class LicenseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_id", nullable = false, length = 40)
    private String licenseId;

    @Column(nullable = false)
    private int version;

    @Column(name = "op_type", nullable = false, length = 16)
    private String opType;

    @Column(name = "claims_json", nullable = false, columnDefinition = "text")
    private String claimsJson;

    @Column(nullable = false, length = 512)  // RSA-2048 签名(base64url)约 344 字符,Ed25519 约 86
    private String signature;

    @Column(nullable = false, columnDefinition = "text")
    private String lic;

    @Column(nullable = false, length = 64)
    private String operator;

    @Column(length = 256)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static LicenseHistory of(License l, String opType, String operator, String reason) {
        LicenseHistory h = new LicenseHistory();
        h.licenseId = l.getLicenseId();
        h.version = l.getCurrentVersion();
        h.opType = opType;
        h.claimsJson = l.getClaimsJson();
        h.signature = l.getSignature();
        h.lic = l.getLic();
        h.operator = operator;
        h.reason = reason;
        h.createdAt = LocalDateTime.now();
        return h;
    }

    public Long getId() { return id; }
    public String getLicenseId() { return licenseId; }
    public int getVersion() { return version; }
    public String getOpType() { return opType; }
    public String getClaimsJson() { return claimsJson; }
    public String getSignature() { return signature; }
    public String getLic() { return lic; }
    public String getOperator() { return operator; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
