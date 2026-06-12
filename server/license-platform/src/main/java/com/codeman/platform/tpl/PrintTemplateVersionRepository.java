package com.codeman.platform.tpl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrintTemplateVersionRepository extends JpaRepository<PrintTemplateVersion, Long> {
    List<PrintTemplateVersion> findByTemplateCodeOrderByVersionDesc(String templateCode);
    Optional<PrintTemplateVersion> findByTemplateCodeAndVersion(String templateCode, int version);
    void deleteByTemplateCode(String templateCode);
}
