package com.cmssoas.platform.notice.web;

import com.cmssoas.platform.notice.domain.Notice;
import com.cmssoas.platform.notice.domain.UserConsent;
import com.cmssoas.platform.notice.dto.NoticeDtos.ConsentReq;
import com.cmssoas.platform.notice.service.ConsentService;
import com.cmssoas.platform.notice.service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 公开页：最终用户阅读当前生效须知 + 提交同意/撤回（无需登录）。
 * 路径不在 /api/** 下，JwtAuthFilter 自动放行。
 */
@RestController
@RequestMapping("/pub")
public class PublicNoticeController {

    private final NoticeService notices;
    private final ConsentService consents;

    public PublicNoticeController(NoticeService notices, ConsentService consents) {
        this.notices = notices;
        this.consents = consents;
    }

    /** 取当前生效须知（type=TERMS/PRIVACY/NOTICE/ANNOUNCEMENT）。 */
    @GetMapping("/notices/active")
    public Notice active(@RequestParam String type) {
        return notices.active(type);
    }

    @PostMapping("/consents")
    public UserConsent grant(@RequestBody ConsentReq r, HttpServletRequest req) {
        return consents.record(r.tenantCode(), r.subject(), r.noticeId(), "GRANTED",
                r.channel(), NoticeController.clientIp(req), req.getHeader("User-Agent"));
    }

    @PostMapping("/consents/revoke")
    public UserConsent revoke(@RequestBody ConsentReq r, HttpServletRequest req) {
        return consents.record(r.tenantCode(), r.subject(), r.noticeId(), "REVOKED",
                r.channel(), NoticeController.clientIp(req), req.getHeader("User-Agent"));
    }
}
