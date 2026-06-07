package com.cmssoas.platform.catalog.service;

import com.cmssoas.platform.catalog.domain.Plan;
import com.cmssoas.platform.catalog.domain.Subscription;
import com.cmssoas.platform.catalog.dto.CatalogDtos.*;
import com.cmssoas.platform.catalog.repo.PlanRepository;
import com.cmssoas.platform.catalog.repo.SubscriptionRepository;
import com.cmssoas.platform.common.ApiException;
import com.cmssoas.platform.license.dto.LicenseDtos.IssueRequest;
import com.cmssoas.platform.license.dto.LicenseDtos.LicenseDetail;
import com.cmssoas.platform.license.service.LicenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 订阅服务：创建订阅时自动据套餐签发 License（订阅 → 授权 闭环）。 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subRepo;
    private final PlanRepository planRepo;
    private final LicenseService licenseService;
    private final ObjectMapper mapper;
    private final com.cmssoas.platform.common.AuditWriter audit;
    private final com.cmssoas.platform.billing.service.InvoiceService invoiceService;

    public SubscriptionService(SubscriptionRepository subRepo, PlanRepository planRepo,
                               LicenseService licenseService, ObjectMapper mapper,
                               com.cmssoas.platform.common.AuditWriter audit,
                               com.cmssoas.platform.billing.service.InvoiceService invoiceService) {
        this.subRepo = subRepo;
        this.planRepo = planRepo;
        this.licenseService = licenseService;
        this.mapper = mapper;
        this.audit = audit;
        this.invoiceService = invoiceService;
    }

    public List<PlanView> plans() {
        return planRepo.findAllByOrderBySortAsc().stream().map(PlanView::from).toList();
    }

    public List<SubscriptionView> list() {
        return subRepo.findAllByOrderByCreatedAtDesc().stream().map(SubscriptionView::from).toList();
    }

    @Transactional
    public SubscriptionView create(CreateSubscriptionRequest r) {
        Plan plan = planRepo.findByCode(r.planCode())
                .orElseThrow(() -> ApiException.badRequest("套餐不存在：" + r.planCode()));
        if (!r.endAt().isAfter(r.startAt())) throw ApiException.badRequest("结束日期必须晚于开始日期");
        int qty = r.qty() == null ? 1 : Math.max(1, r.qty());

        // —— 据套餐自动签发 License ——
        List<String> modules = plan.getModules().isBlank() ? List.of() : Arrays.asList(plan.getModules().split(","));
        Map<String, Object> features = readJson(plan.getFeatures());
        IssueRequest issue = new IssueRequest(
                r.tenantCode(), r.customer(), "CMSSOAS", plan.getCode(), "HYBRID",
                modules, features, plan.getVersionRange(),
                r.startAt(), r.endAt(), plan.getSeats() * qty,
                "订阅自动签发：" + plan.getCode() + " x" + qty);
        LicenseDetail lic = licenseService.issue(issue);

        Subscription s = new Subscription();
        s.setTenantCode(r.tenantCode());
        s.setCustomer(r.customer());
        s.setPlanCode(plan.getCode());
        s.setQty(qty);
        s.setStartAt(r.startAt());
        s.setEndAt(r.endAt());
        s.setStatus("ACTIVE");
        s.setLicenseId(lic.licenseId());
        s.setCreatedAt(LocalDateTime.now());
        subRepo.save(s);

        // —— 自动出账 ——
        invoiceService.bill(r.tenantCode(), r.customer(), s.getId(), plan.getCode(),
                "SUBSCRIBE", plan.getPrice() * qty, r.startAt() + " ~ " + r.endAt());

        audit.log(null, "SUBSCRIPTION_CREATE",
                r.tenantCode() + " · " + plan.getCode() + " x" + qty + " -> " + lic.licenseId());
        log.info("[subscription] 租户 {} 订阅 {} x{} -> 自动签发 {}",
                r.tenantCode(), plan.getCode(), qty, lic.licenseId());
        return SubscriptionView.from(s);
    }

    /** 套餐变更（升/降级）：按新套餐重签关联 License（版本+1），更新订阅。 */
    @Transactional
    public SubscriptionView changePlan(Long id, String newPlanCode) {
        Subscription s = subRepo.findById(id).orElseThrow(() -> ApiException.notFound("订阅不存在"));
        Plan plan = planRepo.findByCode(newPlanCode)
                .orElseThrow(() -> ApiException.badRequest("套餐不存在：" + newPlanCode));
        if (s.getLicenseId() != null) {
            List<String> modules = plan.getModules().isBlank() ? List.of() : Arrays.asList(plan.getModules().split(","));
            licenseService.modify(s.getLicenseId(), new com.cmssoas.platform.license.dto.LicenseDtos.ModifyRequest(
                    modules, readJson(plan.getFeatures()), plan.getVersionRange(),
                    plan.getSeats() * s.getQty(), plan.getCode(), "套餐变更：" + s.getPlanCode() + " → " + plan.getCode()));
        }
        String old = s.getPlanCode();
        s.setPlanCode(plan.getCode());
        subRepo.save(s);
        invoiceService.bill(s.getTenantCode(), s.getCustomer(), s.getId(), plan.getCode(),
                "CHANGE", plan.getPrice() * s.getQty(), "套餐变更 " + old + " → " + plan.getCode());
        audit.log(null, "SUBSCRIPTION_CHANGE", s.getTenantCode() + " · " + old + " → " + plan.getCode()
                + "（重签 " + s.getLicenseId() + "）");
        return SubscriptionView.from(s);
    }

    /** 退订：吊销关联 License，订阅置 CANCELLED。 */
    @Transactional
    public SubscriptionView cancel(Long id) {
        Subscription s = subRepo.findById(id).orElseThrow(() -> ApiException.notFound("订阅不存在"));
        if (s.getLicenseId() != null) {
            licenseService.revoke(s.getLicenseId(),
                    new com.cmssoas.platform.license.dto.LicenseDtos.RevokeRequest("退订：" + s.getTenantCode()));
        }
        s.setStatus("CANCELLED");
        subRepo.save(s);
        audit.log(null, "SUBSCRIPTION_CANCEL", s.getTenantCode() + " · 退订并吊销 " + s.getLicenseId());
        return SubscriptionView.from(s);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String s) {
        try { return s == null || s.isBlank() ? Map.of() : mapper.readValue(s, Map.class); }
        catch (Exception e) { return Map.of(); }
    }
}
