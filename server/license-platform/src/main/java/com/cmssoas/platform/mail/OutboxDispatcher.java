package com.cmssoas.platform.mail;

import com.cmssoas.platform.tenant.domain.EmailOutbox;
import com.cmssoas.platform.tenant.repo.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 发件箱异步投递：定时取出到期 PENDING 邮件投递，失败按指数退避重试。 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final EmailOutboxRepository repo;
    private final OnboardingMailService mailService;

    public OutboxDispatcher(EmailOutboxRepository repo, OnboardingMailService mailService) {
        this.repo = repo;
        this.mailService = mailService;
    }

    @Scheduled(fixedDelayString = "${app.mail.dispatch-interval-ms:5000}")
    @Transactional
    public void dispatch() {
        List<EmailOutbox> due = repo.findTop20ByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
                "PENDING", LocalDateTime.now());
        for (EmailOutbox o : due) {
            try {
                mailService.deliver(o);
                o.setStatus("SENT");
                o.setSentAt(LocalDateTime.now());
                o.setLastError(null);
            } catch (Exception ex) {
                o.setAttempts(o.getAttempts() + 1);
                o.setLastError(trunc(ex.getMessage()));
                if (o.getAttempts() >= o.getMaxAttempts()) {
                    o.setStatus("FAILED");
                    log.error("[outbox] #{} 投递失败，已达上限 -> FAILED: {}", o.getId(), ex.getMessage());
                } else {
                    long backoff = (long) Math.pow(2, o.getAttempts()) * 30; // 60s,120s,240s...
                    o.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff));
                    log.warn("[outbox] #{} 投递失败，{}s 后重试({}/{})", o.getId(), backoff,
                            o.getAttempts(), o.getMaxAttempts());
                }
            }
            repo.save(o);
        }
    }

    private String trunc(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
