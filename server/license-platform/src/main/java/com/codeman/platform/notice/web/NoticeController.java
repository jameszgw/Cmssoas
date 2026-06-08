package com.codeman.platform.notice.web;

import com.codeman.platform.notice.domain.Notice;
import com.codeman.platform.notice.domain.UserConsent;
import com.codeman.platform.notice.dto.NoticeDtos.CreateReq;
import com.codeman.platform.notice.dto.NoticeDtos.UpdateReq;
import com.codeman.platform.notice.service.ConsentService;
import com.codeman.platform.notice.service.NoticeService;
import com.codeman.platform.rbac.service.CurrentUser;
import com.codeman.platform.rbac.service.RequirePerm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 须知与授权管理（运营后台）。 */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService notices;
    private final ConsentService consents;

    public NoticeController(NoticeService notices, ConsentService consents) {
        this.notices = notices;
        this.consents = consents;
    }

    @GetMapping
    @RequirePerm("notice:view")
    public List<Notice> list() {
        return notices.list();
    }

    @PostMapping
    @RequirePerm("notice:edit")
    public Notice create(@RequestBody CreateReq r) {
        return notices.create(r.type(), r.title(), r.contentHtml(), r.forceAck());
    }

    @PutMapping("/{id}")
    @RequirePerm("notice:edit")
    public Notice update(@PathVariable Long id, @RequestBody UpdateReq r) {
        return notices.update(id, r.title(), r.contentHtml(), r.forceAck());
    }

    @PostMapping("/{id}/publish")
    @RequirePerm("notice:edit")
    public Notice publish(@PathVariable Long id) {
        return notices.publish(id);
    }

    /** 当前登录用户待强制确认的须知（控制台登录后阻断弹层用）。 */
    @GetMapping("/pending")
    public List<Notice> pending() {
        CurrentUser.Ctx c = CurrentUser.get();
        return consents.pendingFor(c == null ? "anonymous" : c.username());
    }

    /** 当前登录用户确认（同意）某须知。 */
    @PostMapping("/{id}/ack")
    public UserConsent ack(@PathVariable Long id, HttpServletRequest req) {
        CurrentUser.Ctx c = CurrentUser.get();
        String subject = c == null ? "anonymous" : c.username();
        return consents.record(null, subject, id, "GRANTED", "WEB", clientIp(req), req.getHeader("User-Agent"));
    }

    @GetMapping("/consents")
    @RequirePerm("consent:view")
    public List<UserConsent> consents() {
        return consents.list();
    }

    @GetMapping("/consents/export.csv")
    @RequirePerm("consent:view")
    public ResponseEntity<byte[]> exportConsents() {
        var rows = consents.list().stream().map(c -> java.util.List.<Object>of(
                c.getId(), c.getSubject(), c.getNoticeType(), c.getVersion(), c.getAction(),
                c.getChannel(), c.getIp() == null ? "" : c.getIp(), c.getCreatedAt())).toList();
        return com.codeman.platform.common.CsvUtil.response("consents.csv",
                java.util.List.of("id", "subject", "noticeType", "version", "action", "channel", "ip", "createdAt"),
                rows);
    }

    static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
