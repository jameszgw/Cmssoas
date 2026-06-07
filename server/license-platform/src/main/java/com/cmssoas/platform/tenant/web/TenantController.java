package com.cmssoas.platform.tenant.web;

import com.cmssoas.platform.tenant.dto.OnboardTenantRequest;
import com.cmssoas.platform.tenant.dto.OnboardTenantResponse;
import com.cmssoas.platform.tenant.dto.TenantView;
import com.cmssoas.platform.tenant.repo.TenantRepository;
import com.cmssoas.platform.tenant.service.TenantOnboardingService;
import com.cmssoas.platform.tenant.service.TenantSchemaService;
import com.cmssoas.platform.rbac.service.RequirePerm;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantOnboardingService onboardingService;
    private final TenantRepository tenantRepo;
    private final TenantSchemaService schemaService;

    public TenantController(TenantOnboardingService onboardingService, TenantRepository tenantRepo,
                            TenantSchemaService schemaService) {
        this.onboardingService = onboardingService;
        this.tenantRepo = tenantRepo;
        this.schemaService = schemaService;
    }

    /** 开通租户：建库/初始化/创建超管 + 发送开通邮件。 */
    @PostMapping
    @RequirePerm("tenant:onboard")
    public OnboardTenantResponse onboard(@Valid @RequestBody OnboardTenantRequest req) {
        return onboardingService.onboard(req);
    }

    /** 租户列表。 */
    @GetMapping
    @RequirePerm("tenant:view")
    public List<TenantView> list() {
        return tenantRepo.findAllByOrderByCreatedAtDesc().stream().map(TenantView::from).toList();
    }

    /** 读取某租户独立 Schema 内的配置（证明按租户路由 + 物理隔离）。 */
    @GetMapping("/{code}/settings")
    @RequirePerm("tenant:view")
    public Map<String, Object> settings(@PathVariable String code) {
        return Map.of(
                "tenantCode", code,
                "schema", schemaService.schemaOf(code),
                "settings", schemaService.settings(code),
                "roleCount", schemaService.roleCount(code));
    }
}
