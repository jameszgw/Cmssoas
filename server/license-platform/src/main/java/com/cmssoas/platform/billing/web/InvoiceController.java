package com.cmssoas.platform.billing.web;

import com.cmssoas.platform.billing.domain.Invoice;
import com.cmssoas.platform.billing.domain.TaxInvoice;
import com.cmssoas.platform.billing.service.InvoiceService;
import com.cmssoas.platform.billing.service.TaxInvoiceService;
import com.cmssoas.platform.rbac.service.RequirePerm;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService service;
    private final TaxInvoiceService taxService;

    public InvoiceController(InvoiceService service, TaxInvoiceService taxService) {
        this.service = service;
        this.taxService = taxService;
    }

    @GetMapping
    @RequirePerm("billing:view")
    public List<Invoice> list() {
        return service.list();
    }

    @GetMapping("/export.csv")
    @RequirePerm("billing:view")
    public org.springframework.http.ResponseEntity<byte[]> exportCsv() {
        var rows = service.list().stream().map(i -> java.util.List.<Object>of(
                i.getId(), i.getTenantCode(), i.getCustomer(), i.getPlanCode(), i.getType(),
                i.getAmount(), i.getStatus(), i.getInvoiceNo() == null ? "" : i.getInvoiceNo(),
                i.getCreatedAt())).toList();
        return com.cmssoas.platform.common.CsvUtil.response("invoices.csv",
                java.util.List.of("id", "tenantCode", "customer", "planCode", "type", "amount", "status", "invoiceNo", "createdAt"),
                rows);
    }

    @PostMapping("/{id}/pay")
    @RequirePerm("billing:manage")
    public Invoice pay(@PathVariable Long id) {
        return service.pay(id);
    }

    @PostMapping("/{id}/issue")
    @RequirePerm("billing:manage")
    public Invoice issue(@PathVariable Long id) {
        return service.issueInvoice(id);
    }

    /** 申请开具正规电子发票(抬头/税号/票种/邮箱)。 */
    @PostMapping("/{id}/e-invoice")
    @RequirePerm("billing:manage")
    public TaxInvoice eInvoice(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taxService.apply(id, body.get("title"), body.get("taxNo"), body.get("type"), body.get("email"));
    }

    /** 某账单的开票记录。 */
    @GetMapping("/{id}/tax-invoices")
    @RequirePerm("billing:view")
    public List<TaxInvoice> taxInvoices(@PathVariable Long id) {
        return taxService.byInvoice(id);
    }

    /** 全部电子发票。 */
    @GetMapping("/tax-invoices/all")
    @RequirePerm("billing:view")
    public List<TaxInvoice> allTaxInvoices() {
        return taxService.list();
    }
}
