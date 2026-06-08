package com.codeman.platform.tenant.repo;

import com.codeman.platform.tenant.domain.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
