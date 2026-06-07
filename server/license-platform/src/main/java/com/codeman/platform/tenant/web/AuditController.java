package com.codeman.platform.tenant.web;

import com.codeman.platform.rbac.service.RequirePerm;
import com.codeman.platform.tenant.repo.AuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 审计追溯：运营动作日志（开通、激活等）。 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @RequirePerm("audit:view")
    public List<Map<String, Object>> list() {
        return repo.findTop200ByOrderByCreatedAtDesc().stream().map(a -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("actor", a.getActor());
            m.put("action", a.getAction());
            m.put("detail", a.getDetail());
            m.put("tenantId", a.getTenantId());
            m.put("createdAt", a.getCreatedAt().toString());
            return m;
        }).toList();
    }

    @GetMapping("/export.csv")
    @RequirePerm("audit:view")
    public org.springframework.http.ResponseEntity<byte[]> exportCsv() {
        var rows = repo.findTop200ByOrderByCreatedAtDesc().stream().map(a -> java.util.List.<Object>of(
                a.getCreatedAt().toString(), a.getActor(), a.getAction(),
                a.getTenantId() == null ? "" : a.getTenantId(), a.getDetail() == null ? "" : a.getDetail())).toList();
        return com.codeman.platform.common.CsvUtil.response("audit.csv",
                java.util.List.of("createdAt", "actor", "action", "tenantId", "detail"), rows);
    }
}
