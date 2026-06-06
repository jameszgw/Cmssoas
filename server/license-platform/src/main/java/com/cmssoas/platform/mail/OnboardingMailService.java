package com.cmssoas.platform.mail;

import com.cmssoas.platform.config.AppProperties;
import com.cmssoas.platform.tenant.domain.EmailLog;
import com.cmssoas.platform.tenant.domain.Tenant;
import com.cmssoas.platform.tenant.repo.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 开通邮件服务：用 Thymeleaf 渲染邮件 HTML，按配置经 SMTP 发送或落盘（开发态）。
 * 无论成功失败均落 email_log，便于追溯与重发。
 */
@Service
public class OnboardingMailService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingMailService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final TemplateEngine templateEngine;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailLogRepository emailLogRepo;
    private final AppProperties props;

    public OnboardingMailService(TemplateEngine templateEngine,
                                 ObjectProvider<JavaMailSender> mailSenderProvider,
                                 EmailLogRepository emailLogRepo,
                                 AppProperties props) {
        this.templateEngine = templateEngine;
        this.mailSenderProvider = mailSenderProvider;
        this.emailLogRepo = emailLogRepo;
        this.props = props;
    }

    /**
     * 发送开通邮件。
     * @return 是否已投递（SMTP 成功 或 已落盘）
     */
    public boolean sendOnboarding(Tenant tenant, String activationToken) {
        String subject = "【CMSSOAS】您的租户已开通，请激活管理员账户";
        String activationUrl = props.getActivation().getBaseUrl() + "/" + activationToken;
        String html = render(tenant, activationUrl);

        EmailLog elog = new EmailLog();
        elog.setTenantId(tenant.getId());
        elog.setToAddr(tenant.getAdminEmail());
        elog.setSubject(subject);
        elog.setCreatedAt(LocalDateTime.now());

        try {
            if ("smtp".equalsIgnoreCase(props.getMail().getDelivery())) {
                sendSmtp(tenant.getAdminEmail(), subject, html);
                elog.setStatus("SENT");
                log.info("[mail] 开通邮件已通过 SMTP 发送至 {}", tenant.getAdminEmail());
            } else {
                String path = spool(tenant, html);
                elog.setStatus("SPOOLED");
                elog.setRenderedPath(path);
                log.info("[mail] 开通邮件已渲染落盘（delivery=log）：{} -> {}", tenant.getAdminEmail(), path);
            }
            emailLogRepo.save(elog);
            return true;
        } catch (Exception ex) {
            elog.setStatus("FAILED");
            elog.setError(truncate(ex.getMessage()));
            emailLogRepo.save(elog);
            log.error("[mail] 开通邮件发送失败：{}", ex.getMessage());
            return false;
        }
    }

    private String render(Tenant tenant, String activationUrl) {
        Context ctx = new Context(Locale.SIMPLIFIED_CHINESE);
        ctx.setVariable("tenantName", tenant.getName());
        ctx.setVariable("tenantCode", tenant.getCode());
        ctx.setVariable("version", tenant.getVersion());
        ctx.setVariable("plan", tenant.getPlanCode());
        ctx.setVariable("activationUrl", activationUrl);
        ctx.setVariable("ttlHours", props.getActivation().getTtlHours());
        ctx.setVariable("fromName", props.getMail().getFromName());
        return templateEngine.process("mail/onboarding", ctx);
    }

    private void sendSmtp(String to, String subject, String html) throws Exception {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("未配置 SMTP（spring.mail.host 为空），无法以 smtp 模式发送");
        }
        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(props.getMail().getFrom(), props.getMail().getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        sender.send(msg);
    }

    private String spool(Tenant tenant, String html) throws Exception {
        Path dir = Path.of(props.getMail().getSpoolDir());
        Files.createDirectories(dir);
        String fileName = LocalDateTime.now().format(TS) + "-" + tenant.getCode() + ".html";
        Path file = dir.resolve(fileName);
        Files.writeString(file, html, StandardCharsets.UTF_8);
        return file.toAbsolutePath().toString();
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
