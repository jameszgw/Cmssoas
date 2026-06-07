package com.codeman.platform.portal.service;

import com.codeman.platform.billing.repo.InvoiceRepository;
import com.codeman.platform.billing.repo.PaymentRepository;
import com.codeman.platform.catalog.repo.SubscriptionRepository;
import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.contract.repo.ContractRepository;
import com.codeman.platform.license.repo.LicenseRepository;
import com.codeman.platform.portal.domain.TenantPortal;
import com.codeman.platform.portal.repo.TenantPortalRepository;
import com.codeman.platform.rbac.service.JwtService;
import com.codeman.platform.tenant.domain.Tenant;
import com.codeman.platform.tenant.repo.TenantRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 租户自助门户:运营侧开通/重置访问码;最终客户凭"租户编号 + 访问码"登录,
 * 获得仅限本租户的只读门户(License/账单/合同/订阅 + 关键指标)。
 */
@Service
public class PortalService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RND = new SecureRandom();
    private static final String ROLE = "TENANT_PORTAL";

    private final TenantPortalRepository portalRepo;
    private final TenantRepository tenantRepo;
    private final JwtService jwt;
    private final LicenseRepository licenseRepo;
    private final InvoiceRepository invoiceRepo;
    private final ContractRepository contractRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PaymentRepository paymentRepo;
    private final AuditWriter audit;

    public PortalService(TenantPortalRepository portalRepo, TenantRepository tenantRepo, JwtService jwt,
                         LicenseRepository licenseRepo, InvoiceRepository invoiceRepo, ContractRepository contractRepo,
                         SubscriptionRepository subscriptionRepo, PaymentRepository paymentRepo, AuditWriter audit) {
        this.portalRepo = portalRepo;
        this.tenantRepo = tenantRepo;
        this.jwt = jwt;
        this.licenseRepo = licenseRepo;
        this.invoiceRepo = invoiceRepo;
        this.contractRepo = contractRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.paymentRepo = paymentRepo;
        this.audit = audit;
    }

    // ---- 运营侧 ----
    /** 各租户的门户开通状态(含访问码,供运营查看/分发)。 */
    public List<Map<String, Object>> adminList() {
        Map<String, TenantPortal> byCode = new HashMap<>();
        for (TenantPortal p : portalRepo.findAll()) byCode.put(p.getTenantCode(), p);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Tenant t : tenantRepo.findAllByOrderByCreatedAtDesc()) {
            TenantPortal p = byCode.get(t.getCode());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tenantCode", t.getCode());
            m.put("name", t.getName());
            m.put("enabled", p != null && p.isEnabled());
            m.put("accessCode", p == null ? null : p.getAccessCode());
            out.add(m);
        }
        return out;
    }

    @Transactional
    public TenantPortal enable(String tenantCode) {
        tenantRepo.findByCode(tenantCode).orElseThrow(() -> ApiException.notFound("租户不存在"));
        TenantPortal p = portalRepo.findByTenantCode(tenantCode).orElseGet(TenantPortal::new);
        if (p.getId() == null) {
            p.setTenantCode(tenantCode);
            p.setAccessCode(randomCode());
            p.setCreatedAt(LocalDateTime.now());
        }
        p.setEnabled(true);
        p.setUpdatedAt(LocalDateTime.now());
        portalRepo.save(p);
        audit.log(null, "PORTAL_ENABLE", "租户 " + tenantCode + " 开通自助门户");
        return p;
    }

    @Transactional
    public TenantPortal reset(String tenantCode) {
        TenantPortal p = portalRepo.findByTenantCode(tenantCode)
                .orElseThrow(() -> ApiException.notFound("该租户未开通门户"));
        p.setAccessCode(randomCode());
        p.setUpdatedAt(LocalDateTime.now());
        portalRepo.save(p);
        audit.log(null, "PORTAL_RESET", "租户 " + tenantCode + " 重置门户访问码");
        return p;
    }

    @Transactional
    public void disable(String tenantCode) {
        portalRepo.findByTenantCode(tenantCode).ifPresent(p -> {
            p.setEnabled(false);
            p.setUpdatedAt(LocalDateTime.now());
            portalRepo.save(p);
            audit.log(null, "PORTAL_DISABLE", "租户 " + tenantCode + " 停用自助门户");
        });
    }

    // ---- 门户侧 ----
    /** 门户登录:校验访问码,签发仅限本租户的门户 token。 */
    public Map<String, Object> login(String tenantCode, String accessCode) {
        TenantPortal p = portalRepo.findByTenantCode(tenantCode == null ? "" : tenantCode.trim()).orElse(null);
        if (p == null || !p.isEnabled() || accessCode == null || !p.getAccessCode().equals(accessCode.trim())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "租户编号或访问码不正确");
        }
        Tenant t = tenantRepo.findByCode(p.getTenantCode()).orElseThrow(() -> ApiException.notFound("租户不存在"));
        String token = jwt.generate(0L, p.getTenantCode(), ROLE);
        return Map.of("token", token, "tenantCode", t.getCode(), "tenantName", t.getName());
    }

    /** 解析门户 token,返回 tenantCode;非法或非门户角色抛 401。 */
    public String tenantFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "需要门户登录");
        }
        try {
            Claims c = jwt.parse(authHeader.substring(7));
            if (!ROLE.equals(c.get("role", String.class))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "无效的门户凭证");
            }
            return c.getSubject();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "门户登录已失效,请重新登录");
        }
    }

    /** 本租户只读全景。 */
    public Map<String, Object> overview(String tenantCode) {
        Tenant t = tenantRepo.findByCode(tenantCode).orElseThrow(() -> ApiException.notFound("租户不存在"));

        var licenses = licenseRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> tenantCode.equals(x.getTenantCode())).toList();
        var invoices = invoiceRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> tenantCode.equals(x.getTenantCode())).toList();
        var contracts = contractRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> tenantCode.equals(x.getTenantCode())).toList();
        var subs = subscriptionRepo.findAllByOrderByCreatedAtDesc().stream()
                .filter(x -> tenantCode.equals(x.getTenantCode())).toList();

        long activeLicenses = licenses.stream()
                .filter(x -> x.getStatus() != null && "ACTIVE".equals(x.getStatus().name())).count();
        int pendingAmount = invoices.stream().filter(x -> "PENDING".equals(x.getStatus()))
                .mapToInt(com.codeman.platform.billing.domain.Invoice::getAmount).sum();

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("licenseCount", licenses.size());
        kpi.put("activeLicenses", activeLicenses);
        kpi.put("invoiceCount", invoices.size());
        kpi.put("pendingAmount", pendingAmount);
        kpi.put("contractCount", contracts.size());
        kpi.put("subscriptionCount", subs.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenantCode", t.getCode());
        out.put("tenantName", t.getName());
        out.put("kpi", kpi);
        out.put("licenses", licenses);
        out.put("invoices", invoices);
        out.put("contracts", contracts);
        out.put("subscriptions", subs);
        return out;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
