package com.codeman.platform.harden.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 租户级加固设置:模式(构建自建/平台在线/并存)与默认加固技术。任务可覆盖。 */
@Entity
@Table(name = "harden_config")
public class HardenConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_code", nullable = false, length = 32) private String tenantCode;
    @Column(nullable = false, length = 16) private String mode = "BUILD";   // BUILD/ONLINE/BOTH
    @Column(nullable = false) private boolean obfuscate = true;
    @Column(name = "encrypt_bind", nullable = false) private boolean encryptBind = false;
    @Column(name = "fatjar_encrypt", nullable = false) private boolean fatjarEncrypt = false;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getMode() { return mode; }
    public void setMode(String v) { this.mode = v; }
    public boolean isObfuscate() { return obfuscate; }
    public void setObfuscate(boolean v) { this.obfuscate = v; }
    public boolean isEncryptBind() { return encryptBind; }
    public void setEncryptBind(boolean v) { this.encryptBind = v; }
    public boolean isFatjarEncrypt() { return fatjarEncrypt; }
    public void setFatjarEncrypt(boolean v) { this.fatjarEncrypt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
