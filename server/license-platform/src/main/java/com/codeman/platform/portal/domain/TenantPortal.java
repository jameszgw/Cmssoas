package com.codeman.platform.portal.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 租户自助门户访问:租户编号 + 访问码。最终客户凭此登录只读门户。 */
@Entity
@Table(name = "tenant_portal")
public class TenantPortal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_code", nullable = false, length = 32) private String tenantCode;
    @Column(name = "access_code", nullable = false, length = 32) private String accessCode;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { this.tenantCode = v; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String v) { this.accessCode = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
