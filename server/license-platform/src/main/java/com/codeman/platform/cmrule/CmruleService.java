package com.codeman.platform.cmrule;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.license.dto.LicenseDtos;
import com.codeman.platform.license.dto.LicenseDtos.LicenseDetail;
import com.codeman.platform.license.dto.LicenseDtos.LicenseView;
import com.codeman.platform.license.service.LicenseService;
import com.codeman.platform.tenant.domain.AuditLog;
import com.codeman.platform.tenant.repo.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codeman.platform.cmrule.CmruleEditions.*;

/**
 * CmRuleEngine 商业授权集成:按 CmRuleEngine 四档版本(社区/专业/企业/旗舰)签发 License,
 * features 固化「档位预设 ∪ 合同微调」能力表,客户端经 resolve(edition, features) 得到一致门禁;
 * 并提供 CmRuleEngine 维度的审计查询(本产品 License 的签发/续期/变更/吊销/到期全轨迹)。
 */
@Service
public class CmruleService {

    private final LicenseService licenseService;
    private final AuditLogRepository auditRepo;
    private final AuditWriter audit;

    public CmruleService(LicenseService licenseService, AuditLogRepository auditRepo, AuditWriter audit) {
        this.licenseService = licenseService;
        this.auditRepo = auditRepo;
        this.audit = audit;
    }

    // ---------- 档位 ----------
    public CmruleDtos.EditionsView editions() {
        List<CmruleDtos.EditionView> list = PRESETS.entrySet().stream()
                .map(e -> new CmruleDtos.EditionView(e.getKey(), e.getValue(), resolve(e.getKey(), null)))
                .toList();
        return new CmruleDtos.EditionsView(PRODUCT_CODE, DEFAULT_VERSION_RANGE, CAPABILITY_KEYS, list);
    }

    // ---------- 签发 ----------
    @Transactional
    public LicenseDetail issue(CmruleDtos.IssueRequest r) {
        String edition = r.edition().trim().toUpperCase();
        if (!PRESETS.containsKey(edition)) {
            throw ApiException.badRequest("未知 CmRuleEngine 版本档位：" + r.edition() + "（可选 " + String.join("/", PRESETS.keySet()) + "）");
        }
        Map<String, Boolean> overrides = r.overrides() == null ? Map.of() : r.overrides();
        for (String k : overrides.keySet()) {
            if (!CAPABILITY_KEYS.contains(k)) {
                throw ApiException.badRequest("未知 CmRuleEngine 能力键：" + k);
            }
        }
        // features = 档位预设 ∪ 微调(显式键固化进 License,客户端 resolve 时显式键优先)
        Map<String, Object> features = new LinkedHashMap<>(PRESETS.get(edition));
        overrides.forEach((k, v) -> { if (v != null) features.put(k, v); });

        LicenseDetail d = licenseService.issue(new LicenseDtos.IssueRequest(
                r.tenantCode(), r.customer(), PRODUCT_CODE, edition,
                orElse(r.mode(), "OFFLINE"), EDITION_MODULES.get(edition), features,
                orElse(r.appVersionRange(), DEFAULT_VERSION_RANGE),
                r.notBefore(), r.notAfter(), r.concurrency() == null ? 1 : r.concurrency(),
                orElse(r.reason(), "CmRuleEngine 控制台签发")));
        audit.log(null, "CMRULE_LICENSE_ISSUE",
                d.licenseId() + " · " + edition + " · " + r.customer()
                        + (overrides.isEmpty() ? "" : " · 微调 " + overrides));
        return d;
    }

    // ---------- 查询 ----------
    public List<LicenseView> licenses() {
        return licenseService.list().stream()
                .filter(v -> PRODUCT_CODE.equals(v.productCode()))
                .toList();
    }

    // ---------- 审计查询 ----------
    /**
     * CmRuleEngine 维度审计:CMRULE_* 动作 + 涉及本产品 License 编号的通用 License 事件
     * (LICENSE_ISSUE/RENEW/MODIFY/REVOKE/EXPIRED 等),支持 动作/关键字/时间段 过滤与分页。
     */
    public CmruleDtos.AuditPage auditQuery(String action, String keyword,
                                           LocalDate from, LocalDate to, int page, int size) {
        List<String> ids = licenses().stream().map(LicenseView::licenseId).limit(400).toList();
        Specification<AuditLog> spec = (root, q, cb) -> {
            List<Predicate> scope = new ArrayList<>();
            scope.add(cb.like(root.get("action"), "CMRULE_%"));
            for (String id : ids) scope.add(cb.like(root.get("detail"), "%" + id + "%"));
            List<Predicate> and = new ArrayList<>();
            and.add(cb.or(scope.toArray(new Predicate[0])));
            if (action != null && !action.isBlank()) and.add(cb.equal(root.get("action"), action.trim()));
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                and.add(cb.or(cb.like(cb.lower(root.get("detail")), kw),
                        cb.like(cb.lower(root.get("actor")), kw),
                        cb.like(cb.lower(root.get("action")), kw)));
            }
            if (from != null) and.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) and.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(and.toArray(new Predicate[0]));
        };
        Page<AuditLog> p = auditRepo.findAll(spec, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 500),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        List<CmruleDtos.AuditRow> rows = p.getContent().stream().map(a -> new CmruleDtos.AuditRow(
                a.getId(), a.getActor(), a.getAction(), a.getDetail(), a.getTenantId(), a.getCreatedAt().toString())).toList();
        return new CmruleDtos.AuditPage(p.getTotalElements(), rows);
    }

    private static String orElse(String v, String def) { return (v == null || v.isBlank()) ? def : v; }
}
