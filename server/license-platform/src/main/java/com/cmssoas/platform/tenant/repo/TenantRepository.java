package com.cmssoas.platform.tenant.repo;

import com.cmssoas.platform.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsByCode(String code);
    List<Tenant> findAllByOrderByCreatedAtDesc();
}
