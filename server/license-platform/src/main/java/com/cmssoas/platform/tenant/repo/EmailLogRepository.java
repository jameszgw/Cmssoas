package com.cmssoas.platform.tenant.repo;

import com.cmssoas.platform.tenant.domain.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
