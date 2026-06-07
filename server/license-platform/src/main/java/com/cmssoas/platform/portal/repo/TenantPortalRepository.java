package com.cmssoas.platform.portal.repo;

import com.cmssoas.platform.portal.domain.TenantPortal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantPortalRepository extends JpaRepository<TenantPortal, Long> {
    Optional<TenantPortal> findByTenantCode(String tenantCode);
}
