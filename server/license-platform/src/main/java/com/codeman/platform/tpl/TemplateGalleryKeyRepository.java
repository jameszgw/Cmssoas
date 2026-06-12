package com.codeman.platform.tpl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateGalleryKeyRepository extends JpaRepository<TemplateGalleryKey, Long> {
    Optional<TemplateGalleryKey> findByTenantCode(String tenantCode);
    Optional<TemplateGalleryKey> findByGalleryKey(String galleryKey);
}
