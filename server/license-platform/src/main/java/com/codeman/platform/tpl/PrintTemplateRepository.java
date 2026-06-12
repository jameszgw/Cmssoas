package com.codeman.platform.tpl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrintTemplateRepository extends JpaRepository<PrintTemplate, Long> {
    Optional<PrintTemplate> findByCode(String code);
    List<PrintTemplate> findAllByOrderByUpdatedAtDesc();
    List<PrintTemplate> findByStatusOrderByUpdatedAtDesc(String status);
}
