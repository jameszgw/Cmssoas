package com.cmssoas.platform.tenant.repo;

import com.cmssoas.platform.tenant.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
