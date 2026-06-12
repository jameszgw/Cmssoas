package com.codeman.platform.tpl;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 按租户的模板库拉取密钥(tenant_code='PUBLIC' 为平台公共库)。 */
@Entity
@Table(name = "template_gallery_key")
public class TemplateGalleryKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, unique = true, length = 32)
    private String tenantCode;

    @Column(name = "gallery_key", nullable = false, unique = true, length = 64)
    private String galleryKey;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String v) { tenantCode = v; }
    public String getGalleryKey() { return galleryKey; }
    public void setGalleryKey(String v) { galleryKey = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
