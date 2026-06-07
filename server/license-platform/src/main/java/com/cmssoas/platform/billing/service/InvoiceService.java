package com.cmssoas.platform.billing.service;

import com.cmssoas.platform.billing.domain.Invoice;
import com.cmssoas.platform.billing.repo.InvoiceRepository;
import com.cmssoas.platform.common.ApiException;
import com.cmssoas.platform.common.AuditWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 账单服务：订阅/变更自动出账；支持收款与开票。 */
@Service
public class InvoiceService {

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InvoiceRepository repo;
    private final AuditWriter audit;

    public InvoiceService(InvoiceRepository repo, AuditWriter audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** 自动出账（订阅/变更/续期触发）。 */
    @Transactional
    public Invoice bill(String tenantCode, String customer, Long subscriptionId, String planCode,
                        String type, int amount, String period) {
        Invoice in = new Invoice();
        in.setTenantCode(tenantCode);
        in.setCustomer(customer);
        in.setSubscriptionId(subscriptionId);
        in.setPlanCode(planCode);
        in.setType(type);
        in.setAmount(amount);
        in.setCurrency("CNY");
        in.setPeriod(period);
        in.setStatus("PENDING");
        in.setCreatedAt(LocalDateTime.now());
        repo.save(in);
        audit.log(null, "INVOICE_CREATE", tenantCode + " · " + type + " · ¥" + amount + "（账单#" + in.getId() + "）");
        return in;
    }

    public List<Invoice> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Invoice pay(Long id) {
        Invoice in = require(id);
        if (!"PENDING".equals(in.getStatus())) throw ApiException.badRequest("仅待支付账单可收款");
        in.setStatus("PAID");
        in.setPaidAt(LocalDateTime.now());
        repo.save(in);
        audit.log(null, "INVOICE_PAID", "账单#" + id + " 已收款 ¥" + in.getAmount());
        return in;
    }

    @Transactional
    public Invoice issueInvoice(Long id) {
        Invoice in = require(id);
        if (!"PAID".equals(in.getStatus())) throw ApiException.badRequest("仅已收款账单可开票");
        in.setStatus("INVOICED");
        in.setInvoiceNo("INV-" + LocalDateTime.now().format(NO) + "-" + id);
        in.setInvoicedAt(LocalDateTime.now());
        repo.save(in);
        audit.log(null, "INVOICE_ISSUED", "账单#" + id + " 已开票 " + in.getInvoiceNo());
        return in;
    }

    private Invoice require(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("账单不存在"));
    }
}
