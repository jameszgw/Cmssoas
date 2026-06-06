package com.cmssoas.platform.mail;

import com.cmssoas.platform.config.AppProperties;
import com.cmssoas.platform.tenant.domain.EmailLog;
import com.cmssoas.platform.tenant.domain.EmailOutbox;
import com.cmssoas.platform.tenant.domain.Tenant;
import com.cmssoas.platform.tenant.repo.EmailLogRepository;
import com.cmssoas.platform.tenant.repo.EmailOutboxRepository;
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
 * 开通邮件服务（事务发件箱模式）：
 *  - enqueueOnboarding：渲染 HTML 并写入 email_outbox（与租户创建同事务，原子）。
 *  - deliver：实际投递（SMTP 或落盘），由 OutboxDispatcher 异步调用并重试。
 */
@Service
public class OnboardingMailService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingMailService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    public static final String SUBJECT = "【CMSSOAS】您的租户已开通，请激活管理员账户";

    private final TemplateEngine templateEngine;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailLogRepository emailLogRepo;
    private final EmailOutboxRepository outboxRepo;
    private final AppProperties props;

    public OnboardingMailService(TemplateEngine templateEngine,
                                 ObjectProvider<JavaMailSender> mailSenderProvider,
                                 EmailLogRepository emailLogRepo,
                                 EmailOutboxRepository outboxRepo,
                                 AppProperties props) {
        this.templateEngine = templateEngine;
        this.mailSenderProvider = mailSenderProvider;
        this.emailLogRepo = emailLogRepo;
        this.outboxRepo = outboxRepo;
        this.props = props;
    }

    /** 入队（事务内）：返回是否已入队。 */
    public boolean enqueueOnboarding(Tenant tenant, String activationToken) {
        String activationUrl = props.getActivation().getBaseUrl() + "/" + activationToken;
        String html = render(tenant, activationUrl);
        EmailOutbox o = new EmailOutbox();
        o.setTenantId(tenant.getId());
        o.setToAddr(tenant.getAdminEmail());
        o.setSubject(SUBJECT);
        o.setBodyHtml(html);
        o.setStatus("PENDING");
        o.setAttempts(0);
        o.setMaxAttempts(5);
        o.setNextAttemptAt(LocalDateTime.now());
        o.setCreatedAt(LocalDateTime.now());
        outboxRepo.save(o);
        log.info("[mail] 开通邮件已入发件箱(outbox) -> {}", tenant.getAdminEmail());
        return true;
    }

    /** 实际投递；失败抛异常（由 dispatcher 处理重试）。 */
    public void deliver(EmailOutbox o) throws Exception {
        if ("smtp".equalsIgnoreCase(props.getMail().getDelivery())) {
            sendSmtp(o.getToAddr(), o.getSubject(), o.getBodyHtml());
            record(o, "SENT", null);
            log.info("[mail] outbox#{} 经 SMTP 发送至 {}", o.getId(), o.getToAddr());
        } else {
            String path = spool(o);
            record(o, "SPOOLED", path);
            log.info("[mail] outbox#{} 已落盘 {}", o.getId(), path);
        }
    }

    private void record(EmailOutbox o, String status, String path) {
        EmailLog e = new EmailLog();
        e.setTenantId(o.getTenantId());
        e.setToAddr(o.getToAddr());
        e.setSubject(o.getSubject());
        e.setStatus(status);
        e.setRenderedPath(path);
        e.setCreatedAt(LocalDateTime.now());
        emailLogRepo.save(e);
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
        if (sender == null) throw new IllegalStateException("未配置 SMTP（spring.mail.host 为空）");
        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(props.getMail().getFrom(), props.getMail().getFromName());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        sender.send(msg);
    }

    private String spool(EmailOutbox o) throws Exception {
        Path dir = Path.of(props.getMail().getSpoolDir());
        Files.createDirectories(dir);
        Path file = dir.resolve(LocalDateTime.now().format(TS) + "-outbox" + o.getId() + ".html");
        Files.writeString(file, o.getBodyHtml(), StandardCharsets.UTF_8);
        return file.toAbsolutePath().toString();
    }
}
