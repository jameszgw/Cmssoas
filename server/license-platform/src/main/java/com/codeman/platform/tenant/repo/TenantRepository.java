package com.codeman.platform.tenant.repo;

import com.codeman.platform.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsByCode(String code);
    List<Tenant> findAllByOrderByCreatedAtDesc();
    Optional<Tenant> findByCode(String code);
}
