package com.cmssoas.platform.notice.service;

import com.cmssoas.platform.common.AuditWriter;
import com.cmssoas.platform.notice.domain.Notice;
import com.cmssoas.platform.notice.domain.UserConsent;
import com.cmssoas.platform.notice.repo.NoticeRepository;
import com.cmssoas.platform.notice.repo.UserConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 用户授权(同意)服务：记录同意/撤回，留存 IP/UA/版本/时间供取证；可查询待确认的强制须知。 */
@Service
public class ConsentService {

    private final UserConsentRepository repo;
    private final NoticeRepository noticeRepo;
    private final AuditWriter audit;

    public ConsentService(UserConsentRepository repo, NoticeRepository noticeRepo, AuditWriter audit) {
        this.repo = repo;
        this.noticeRepo = noticeRepo;
        this.audit = audit;
    }

    @Transactional
    public UserConsent record(String tenantCode, String subject, Long noticeId, String action,
                              String channel, String ip, String userAgent) {
        Notice n = noticeRepo.findById(noticeId)
                .orElseThrow(() -> com.cmssoas.platform.common.ApiException.notFound("须知不存在"));
        UserConsent c = new UserConsent();
        c.setTenantCode(tenantCode);
        c.setSubject(subject);
        c.setNoticeId(noticeId);
        c.setNoticeType(n.getType());
        c.setVersion(n.getVersion());
        c.setAction(action);
        c.setChannel(channel == null ? "WEB" : channel);
        c.setIp(ip);
        c.setUserAgent(userAgent != null && userAgent.length() > 256 ? userAgent.substring(0, 256) : userAgent);
        c.setCreatedAt(LocalDateTime.now());
        repo.save(c);
        audit.log(null, "CONSENT_" + action, subject + " · " + n.getType() + " v" + n.getVersion());
        return c;
    }

    public List<UserConsent> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<UserConsent> listBySubject(String subject) {
        return repo.findBySubjectOrderByCreatedAtDesc(subject);
    }

    /** 主体尚未有效同意的“强制确认”已发布须知（force_ack 且最新记录非 GRANTED）。 */
    public List<Notice> pendingFor(String subject) {
        List<Notice> pending = new ArrayList<>();
        for (Notice n : noticeRepo.findByStatusAndForceAckTrue("PUBLISHED")) {
            UserConsent last = repo.findFirstBySubjectAndNoticeIdOrderByCreatedAtDesc(subject, n.getId());
            if (last == null || !"GRANTED".equals(last.getAction())) pending.add(n);
        }
        return pending;
    }
}
