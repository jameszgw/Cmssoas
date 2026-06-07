package com.cmssoas.platform.portal.web;

import com.cmssoas.platform.portal.domain.TenantPortal;
import com.cmssoas.platform.portal.service.PortalService;
import com.cmssoas.platform.rbac.service.RequirePerm;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 运营侧:租户自助门户开通/管理。 */
@RestController
@RequestMapping("/api/portal-admin")
public class PortalAdminController {

    private final PortalService service;

    public PortalAdminController(PortalService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("tenant:view")
    public List<Map<String, Object>> list() {
        return service.adminList();
    }

    @PostMapping("/{tenantCode}/enable")
    @RequirePerm("tenant:portal")
    public TenantPortal enable(@PathVariable String tenantCode) {
        return service.enable(tenantCode);
    }

    @PostMapping("/{tenantCode}/reset")
    @RequirePerm("tenant:portal")
    public TenantPortal reset(@PathVariable String tenantCode) {
        return service.reset(tenantCode);
    }

    @PostMapping("/{tenantCode}/disable")
    @RequirePerm("tenant:portal")
    public Map<String, Object> disable(@PathVariable String tenantCode) {
        service.disable(tenantCode);
        return Map.of("ok", true);
    }
}
