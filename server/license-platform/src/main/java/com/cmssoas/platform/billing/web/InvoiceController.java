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
