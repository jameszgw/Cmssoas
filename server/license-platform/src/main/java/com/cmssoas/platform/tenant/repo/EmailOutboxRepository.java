package com.cmssoas.platform.tenant.repo;

import com.cmssoas.platform.tenant.domain.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {
    List<EmailOutbox> findTop20ByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
            String status, LocalDateTime before);
}
