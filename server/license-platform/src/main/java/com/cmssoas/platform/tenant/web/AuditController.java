package com.cmssoas.platform.tenant.web;

import com.cmssoas.platform.tenant.repo.AuditLogRepository;
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
}
