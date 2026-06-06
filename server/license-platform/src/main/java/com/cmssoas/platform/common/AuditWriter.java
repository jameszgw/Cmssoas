package com.cmssoas.platform.common;

import com.cmssoas.platform.rbac.service.CurrentUser;
import com.cmssoas.platform.tenant.domain.AuditLog;
import com.cmssoas.platform.tenant.repo.AuditLogRepository;
import org.springframework.stereotype.Component;

/** 统一审计写入：自动带上当前登录操作人。 */
@Component
public class AuditWriter {

    private final AuditLogRepository repo;

    public AuditWriter(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(Long tenantId, String action, String detail) {
        CurrentUser.Ctx c = CurrentUser.get();
        String actor = c != null ? c.username() : "system";
        repo.save(AuditLog.of(tenantId, actor, action, detail));
    }
}
