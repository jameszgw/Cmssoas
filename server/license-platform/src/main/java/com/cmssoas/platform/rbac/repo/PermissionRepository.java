package com.cmssoas.platform.rbac.repo;

import com.cmssoas.platform.rbac.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findAllByOrderBySortAsc();
}
