package com.codeman.platform.customer.service;

import com.codeman.platform.billing.repo.InvoiceRepository;
import com.codeman.platform.billing.repo.PaymentRepository;
import com.codeman.platform.catalog.repo.SubscriptionRepository;
import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.contract.repo.ContractRepository;
import com.codeman.platform.customer.domain.Customer;
import com.codeman.platform.customer.repo.CustomerRepository;
import com.codeman.platform.license.repo.LicenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一客户主数据 + 客户360。
 * 客户360 以客户名称聚合 License / 合同 / 账单 / 支付 / 订阅(按各实体 customer 字段匹配),
 * 给出该客户的全景与关键指标——打通原先散落在各模块的客户视角。
 */
@Service
public class CustomerService {

    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CustomerRepository repo;
    private final LicenseRepository licenseRepo;
    private final ContractRepository contractRepo;
    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final AuditWriter audit;

    public CustomerService(CustomerRepository repo, LicenseRepository licenseRepo, ContractRepository contractRepo,
                           InvoiceRepository invoiceRepo, PaymentRepository paymentRepo,
                           SubscriptionRepository subscriptionRepo, AuditWriter audit) {
        this.repo = repo;
        this.licenseRepo = licenseRepo;
        this.contractRepo = contractRepo;
        this.invoiceRepo = invoiceRepo;
        this.paymentRepo = paymentRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.audit = audit;
    }

    public List<Customer> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Customer get(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("客户不存在"));
    }

    @Transactional
    public Customer create(Customer in) {
        if (in.getName() == null || in.getName().isBlank()) throw ApiException.badRequest("客户名称必填");
        if (repo.existsByName(in.getName().trim())) throw ApiException.badRequest("客户名称已存在");
        in.setName(in.getName().trim());
        in.setCode("CUST-" + LocalDateTime.now().format(NO));
        if (in.getStatus() == null || in.getStatus().isBlank()) in.setStatus("ACTIVE");
        in.setCreatedAt(LocalDateTime.now());
        repo.save(in);
        audit.log(null, "CUSTOMER_CREATE", in.getCode() + " · " + in.getName());
        return in;
    }

    @Transactional
    public Customer update(Long id, Customer in) {
        Customer c = get(id);
        c.setContact(in.getContact());
        c.setEmail(in.getEmail());
        c.setPhone(in.getPhone());
        c.setIndustry(in.getIndustry());
        c.setTenantCode(in.getTenantCode());
        c.setNote(in.getNote());
        if (in.getStatus() != null && !in.getStatus().isBlank()) c.setStatus(in.getStatus());
        repo.save(c);
        audit.log(null, "CUSTOMER_UPDATE", "客户#" + id + " 已更新");
        return c;
    }

    /** 客户360:按名称聚合该客户的 License/合同/账单/支付/订阅与关键指标。 */
    public Map<String, Object> overview(Long id) {
        Customer c = get(id);
        String name = c.getName().trim();

        var licenses = licenseRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> name.equalsIgnoreCase(safe(x.getCustomer()))).toList();
        var contracts = contractRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> name.equalsIgnoreCase(safe(x.getCustomer()))).toList();
        var invoices = invoiceRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> name.equalsIgnoreCase(safe(x.getCustomer()))).toList();
        var payments = paymentRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> name.equalsIgnoreCase(safe(x.getCustomer()))).toList();
        var subs = subscriptionRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> name.equalsIgnoreCase(safe(x.getCustomer()))).toList();

        long activeLicenses = licenses.stream()
                .filter(x -> x.getStatus() != null && "ACTIVE".equals(x.getStatus().name())).count();
        int signedAmount = contracts.stream().filter(x -> "SIGNED".equals(x.getStatus()))
                .mapToInt(com.codeman.platform.contract.domain.Contract::getAmount).sum();
        int paidAmount = invoices.stream().filter(x -> !"PENDING".equals(x.getStatus()) && !"VOID".equals(x.getStatus()))
                .mapToInt(com.codeman.platform.billing.domain.Invoice::getAmount).sum();
        int pendingAmount = invoices.stream().filter(x -> "PENDING".equals(x.getStatus()))
                .mapToInt(com.codeman.platform.billing.domain.Invoice::getAmount).sum();

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("licenseCount", licenses.size());
        kpi.put("activeLicenses", activeLicenses);
        kpi.put("contractCount", contracts.size());
        kpi.put("signedAmount", signedAmount);
        kpi.put("invoiceCount", invoices.size());
        kpi.put("paidAmount", paidAmount);
        kpi.put("pendingAmount", pendingAmount);
        kpi.put("subscriptionCount", subs.size());
        kpi.put("paymentCount", payments.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("customer", c);
        out.put("kpi", kpi);
        out.put("licenses", licenses);
        out.put("contracts", contracts);
        out.put("invoices", invoices);
        out.put("payments", payments);
        out.put("subscriptions", subs);
        return out;
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
