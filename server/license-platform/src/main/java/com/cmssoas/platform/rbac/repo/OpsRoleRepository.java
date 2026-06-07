package com.cmssoas.platform.rbac.repo;

import com.cmssoas.platform.rbac.domain.OpsRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpsRoleRepository extends JpaRepository<OpsRole, Long> {
    Optional<OpsRole> findByCode(String code);
}
