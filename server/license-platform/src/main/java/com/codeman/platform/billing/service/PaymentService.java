package com.codeman.platform.billing.service;

import com.codeman.platform.billing.domain.Invoice;
import com.codeman.platform.billing.domain.Payment;
import com.codeman.platform.billing.repo.InvoiceRepository;
import com.codeman.platform.billing.repo.PaymentRepository;
import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 支付/收款编排:从待支付账单发起扫码支付 → 渠道回调/沙箱确认 → 账单收款(PAID),全程幂等。
 * 通过 {@link PaymentProvider} 抽象支持多渠道,默认沙箱({@code app.pay.provider=mock})。
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository repo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceService invoiceService;
    private final AuditWriter audit;
    private final AppProperties props;
    private final Map<String, PaymentProvider> providers;

    public PaymentService(PaymentRepository repo, InvoiceRepository invoiceRepo, InvoiceService invoiceService,
                          AuditWriter audit, AppProperties props, List<PaymentProvider> providerList) {
        this.repo = repo;
        this.invoiceRepo = invoiceRepo;
        this.invoiceService = invoiceService;
        this.audit = audit;
        this.props = props;
        this.providers = providerList.stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.channel().toUpperCase(), p -> p));
    }

    /** 当前启用渠道(按 app.pay.provider;找不到则回退沙箱)。 */
    private PaymentProvider active() {
        PaymentProvider p = providers.get(props.getPay().getProvider().toUpperCase());
        if (p == null) {
            p = providers.get("MOCK");
            if (p == null) throw ApiException.badRequest("未配置可用支付渠道");
        }
        return p;
    }

    public List<Payment> list() { return repo.findAllByOrderByCreatedAtDesc(); }

    public Payment get(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("支付单不存在"));
    }

    public List<Payment> byInvoice(Long invoiceId) {
        return repo.findByInvoiceIdOrderByCreatedAtDesc(invoiceId);
    }

    /** 发起收款:对 PENDING 账单创建支付并向渠道下单,返回二维码/收银台信息。 */
    @Transactional
    public Payment createForInvoice(Long invoiceId) {
        Invoice in = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> ApiException.notFound("账单不存在"));
        if (!"PENDING".equals(in.getStatus())) throw ApiException.badRequest("仅待支付账单可发起收款");

        PaymentProvider provider = active();
        Payment p = new Payment();
        p.setPaymentNo("PAY-" + LocalDateTime.now().format(NO) + "-" + invoiceId);
        p.setInvoiceId(invoiceId);
        p.setTenantCode(in.getTenantCode());
        p.setCustomer(in.getCustomer());
        p.setAmount(in.getAmount());
        p.setCurrency(in.getCurrency());
        p.setChannel(provider.channel());
        p.setStatus("CREATED");
        p.setCreatedAt(LocalDateTime.now());
        repo.save(p);

        PaymentProvider.PrepayResult r = provider.create(p);
        p.setQrContent(r.qrContent());
        p.setPayUrl(r.payUrl());
        p.setProviderTxnId(r.providerTxnId());
        repo.save(p);

        audit.log(null, "PAYMENT_CREATE", "账单#" + invoiceId + " 发起收款 " + p.getPaymentNo()
                + "（" + provider.channel() + " ¥" + p.getAmount() + "）");
        return p;
    }

    /** 沙箱"模拟支付成功"(仅沙箱渠道可用),用于联调/演示。 */
    @Transactional
    public Payment sandboxConfirm(Long id) {
        Payment p = get(id);
        if (!active().sandbox()) throw ApiException.badRequest("当前为真实支付渠道,不支持模拟支付");
        return markPaid(p, "SANDBOX");
    }

    /** 处理渠道异步回调:验签 → 标记支付单与账单已付(幂等)。 */
    @Transactional
    public boolean handleNotify(String channel, String rawBody, Map<String, String> headers) {
        PaymentProvider provider = providers.get(channel == null ? "" : channel.toUpperCase());
        if (provider == null) { log.warn("[pay] 未知回调渠道:{}", channel); return false; }
        String paymentNo = provider.verifyNotify(rawBody, headers);
        if (paymentNo == null) { log.warn("[pay] 回调验签失败:{}", channel); return false; }
        Payment p = repo.findByPaymentNo(paymentNo).orElse(null);
        if (p == null) { log.warn("[pay] 回调支付单不存在:{}", paymentNo); return false; }
        markPaid(p, channel);
        return true;
    }

    /** 幂等:已 PAID 直接返回;否则置 PAID 并联动账单收款。 */
    private Payment markPaid(Payment p, String via) {
        if ("PAID".equals(p.getStatus())) return p;
        p.setStatus("PAID");
        p.setPaidAt(LocalDateTime.now());
        repo.save(p);
        // 联动账单:仅当账单仍 PENDING 才收款,避免重复/异常
        invoiceRepo.findById(p.getInvoiceId()).ifPresent(in -> {
            if ("PENDING".equals(in.getStatus())) invoiceService.pay(in.getId());
        });
        audit.log(null, "PAYMENT_PAID", "支付单 " + p.getPaymentNo() + " 已支付（" + via + " ¥" + p.getAmount() + "）");
        return p;
    }
}
