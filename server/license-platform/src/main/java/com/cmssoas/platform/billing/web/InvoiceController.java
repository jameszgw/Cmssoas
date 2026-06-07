package com.cmssoas.platform.billing.web;

import com.cmssoas.platform.billing.domain.Invoice;
import com.cmssoas.platform.billing.service.InvoiceService;
import com.cmssoas.platform.rbac.service.RequirePerm;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
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
}
