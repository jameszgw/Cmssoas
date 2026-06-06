package com.cmssoas.platform.tenant.web;

import com.cmssoas.platform.tenant.dto.OnboardTenantRequest;
import com.cmssoas.platform.tenant.dto.OnboardTenantResponse;
import com.cmssoas.platform.tenant.dto.TenantView;
import com.cmssoas.platform.tenant.repo.TenantRepository;
import com.cmssoas.platform.tenant.service.TenantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantOnboardingService onboardingService;
    private final TenantRepository tenantRepo;

    public TenantController(TenantOnboardingService onboardingService, TenantRepository tenantRepo) {
        this.onboardingService = onboardingService;
        this.tenantRepo = tenantRepo;
    }

    /** 开通租户：建库/初始化/创建超管 + 发送开通邮件。 */
    @PostMapping
    public OnboardTenantResponse onboard(@Valid @RequestBody OnboardTenantRequest req) {
        return onboardingService.onboard(req);
    }

    /** 租户列表。 */
    @GetMapping
    public List<TenantView> list() {
        return tenantRepo.findAllByOrderByCreatedAtDesc().stream().map(TenantView::from).toList();
    }
}
