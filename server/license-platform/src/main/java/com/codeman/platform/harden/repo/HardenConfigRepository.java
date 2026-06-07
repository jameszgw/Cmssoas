package com.codeman.platform.harden.repo;

import com.codeman.platform.harden.domain.HardenConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HardenConfigRepository extends JpaRepository<HardenConfig, Long> {
    Optional<HardenConfig> findByTenantCode(String tenantCode);
}
