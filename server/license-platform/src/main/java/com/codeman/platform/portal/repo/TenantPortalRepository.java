package com.codeman.platform.portal.repo;

import com.codeman.platform.portal.domain.TenantPortal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantPortalRepository extends JpaRepository<TenantPortal, Long> {
    Optional<TenantPortal> findByTenantCode(String tenantCode);
}
