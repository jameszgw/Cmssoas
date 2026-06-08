package com.codeman.platform.license.service;

import com.codeman.platform.license.domain.License;
import com.codeman.platform.license.domain.LicenseStatus;
import com.codeman.platform.license.repo.LicenseRepository;
import com.codeman.platform.mail.OnboardingMailService;
import com.codeman.platform.tenant.repo.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 到期提醒：定时扫描即将到期(≤N 天)的 ACTIVE License，经发件箱(outbox)异步外发提醒邮件。
 * 以 license.expiry_reminded 去重，避免重复打扰。
 */
@Service
public class LicenseExpiryReminder {

    private static final Logger log = LoggerFactory.getLogger(LicenseExpiryReminder.class);

    private final LicenseRepository licenseRepo;
    private final TenantRepository tenantRepo;
    private final OnboardingMailService mailService;
    private final com.codeman.platform.alert.WeComNotifier weCom;
    private final int days;
    private final String fallbackTo;

    public LicenseExpiryReminder(LicenseRepository licenseRepo, TenantRepository tenantRepo,
                                 OnboardingMailService mailService,
                                 com.codeman.platform.alert.WeComNotifier weCom,
                                 @Value("${app.license.expiry-reminder-days:30}") int days,
                                 @Value("${app.license.expiry-reminder-fallback-to:ops@codeman.com}") String fallbackTo) {
        this.licenseRepo = licenseRepo;
        this.tenantRepo = tenantRepo;
        this.mailService = mailService;
        this.weCom = weCom;
        this.days = days;
        this.fallbackTo = fallbackTo;
    }

    /** 每天扫描一次（默认）；去重保证只发一次。 */
    @Scheduled(fixedDelayString = "${app.license.expiry-reminder-interval-ms:86400000}", initialDelay = 60000)
    public void scheduled() {
        int n = runOnce();
        if (n > 0) log.info("[expiry] 本轮发送到期提醒 {} 封", n);
    }

    /** 执行一次扫描+入队，返回本次发送数（供手动触发/测试）。 */
    @Transactional
    public int runOnce() {
        LocalDate now = LocalDate.now();
        LocalDate threshold = now.plusDays(days);
        int sent = 0;
        for (License l : licenseRepo.findByStatus(LicenseStatus.ACTIVE)) {
            if (l.isExpiryReminded()) continue;
            // 即将到期(≤N 天) 或 已过期未提醒
            if (l.getNotAfter().isAfter(threshold)) continue;
            String to = tenantRepo.findByCode(l.getTenantCode())
                    .map(t -> t.getAdminEmail()).filter(s -> s != null && !s.isBlank())
                    .orElse(fallbackTo);
            long left = now.until(l.getNotAfter()).getDays();
            String subject = "【CODEMAN】License 到期提醒：" + l.getLicenseId();
            String html = render(l, left);
            mailService.enqueue(to, subject, html, null);
            l.setExpiryReminded(true);
            licenseRepo.save(l);
            sent++;
        }
        if (sent > 0) {
            weCom.sendMarkdown("## ⏳ License 到期提醒\n本轮共 **" + sent + "** 张授权即将到期（≤" + days
                    + " 天），已发送提醒邮件，请关注续费。");
        }
        return sent;
    }

    private String render(License l, long daysLeft) {
        String when = daysLeft < 0 ? "已过期 " + (-daysLeft) + " 天" : daysLeft + " 天后到期";
        return "<div style='font-family:sans-serif'>" +
                "<h2>License 到期提醒</h2>" +
                "<p>尊敬的客户「" + l.getCustomer() + "」：</p>" +
                "<p>您的授权 <b>" + l.getLicenseId() + "</b>（版本 " + l.getEdition() + "）将于 <b>" +
                l.getNotAfter() + "</b>（" + when + "）。为保障业务连续，请及时续期。</p>" +
                "<p>如已续费请忽略本邮件。</p></div>";
    }
}
