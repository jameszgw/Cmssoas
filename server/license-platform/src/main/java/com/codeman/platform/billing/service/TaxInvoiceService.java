package com.codeman.platform.billing.service;

import com.codeman.platform.billing.domain.Invoice;
import com.codeman.platform.billing.domain.TaxInvoice;
import com.codeman.platform.billing.repo.InvoiceRepository;
import com.codeman.platform.billing.repo.TaxInvoiceRepository;
import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 正规税务发票(电子发票)开具:对已收款账单申请开票 → 渠道开具 → 回填发票代码/号码/PDF,
 * 并将计费账单置为已开票(发票号取真实发票号码)。渠道经 {@link EInvoiceProvider} 抽象,默认沙箱。
 */
@Service
public class TaxInvoiceService {

    private final TaxInvoiceRepository repo;
    private final InvoiceRepository invoiceRepo;
    private final AppProperties props;
    private final AuditWriter audit;
    private final Map<String, EInvoiceProvider> providers;

    public TaxInvoiceService(TaxInvoiceRepository repo, InvoiceRepository invoiceRepo, AppProperties props,
                             AuditWriter audit, List<EInvoiceProvider> providerList) {
        this.repo = repo;
        this.invoiceRepo = invoiceRepo;
        this.props = props;
        this.audit = audit;
        this.providers = providerList.stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.provider().toUpperCase(), p -> p));
    }

    private EInvoiceProvider active() {
        EInvoiceProvider p = providers.get(props.getEinvoice().getProvider().toUpperCase());
        if (p == null) p = providers.get("MOCK");
        if (p == null) throw ApiException.badRequest("未配置可用开票渠道");
        return p;
    }

    public List<TaxInvoice> list() { return repo.findAllByOrderByCreatedAtDesc(); }

    public List<TaxInvoice> byInvoice(Long invoiceId) { return repo.findByInvoiceId(invoiceId); }

    /** 申请开具电子发票:账单须已收款(PAID)。开具成功后账单置 INVOICED,发票号取真实发票号码。 */
    @Transactional
    public TaxInvoice apply(Long invoiceId, String title, String taxNo, String type, String email) {
        Invoice in = invoiceRepo.findById(invoiceId).orElseThrow(() -> ApiException.notFound("账单不存在"));
        if (!"PAID".equals(in.getStatus())) throw ApiException.badRequest("仅已收款账单可开票");
        if (title == null || title.isBlank()) throw ApiException.badRequest("发票抬头必填");
        String t = "SPECIAL".equalsIgnoreCase(type) ? "SPECIAL" : "NORMAL";
        if ("SPECIAL".equals(t) && (taxNo == null || taxNo.isBlank()))
            throw ApiException.badRequest("专用发票必须提供购方税号");

        EInvoiceProvider provider = active();
        TaxInvoice ti = new TaxInvoice();
        ti.setInvoiceId(invoiceId);
        ti.setTitle(title.trim());
        ti.setTaxNo(taxNo);
        ti.setType(t);
        ti.setEmail(email);
        ti.setAmount(in.getAmount());
        ti.setProvider(provider.provider());
        ti.setStatus("ISSUING");
        ti.setCreatedAt(LocalDateTime.now());
        repo.save(ti);

        try {
            EInvoiceProvider.IssueResult r = provider.issue(ti);
            ti.setInvoiceCode(r.invoiceCode());
            ti.setInvoiceSerial(r.invoiceSerial());
            ti.setPdfUrl(r.pdfUrl());
            ti.setStatus("ISSUED");
            ti.setIssuedAt(LocalDateTime.now());
            repo.save(ti);

            // 账单联动:置已开票,发票号取真实发票号码
            in.setStatus("INVOICED");
            in.setInvoiceNo(r.invoiceSerial());
            in.setInvoicedAt(LocalDateTime.now());
            invoiceRepo.save(in);

            audit.log(null, "EINVOICE_ISSUED", "账单#" + invoiceId + " 开具" + (t.equals("SPECIAL") ? "专票" : "普票")
                    + " " + r.invoiceCode() + "/" + r.invoiceSerial());
        } catch (Exception e) {
            ti.setStatus("FAILED");
            repo.save(ti);
            audit.log(null, "EINVOICE_FAILED", "账单#" + invoiceId + " 开票失败:" + e.getMessage());
            throw ApiException.badRequest("开票失败:" + e.getMessage());
        }
        return ti;
    }
}
