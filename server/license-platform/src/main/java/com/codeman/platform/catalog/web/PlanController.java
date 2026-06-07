package com.codeman.platform.catalog.web;

import com.codeman.platform.catalog.dto.CatalogDtos.*;
import com.codeman.platform.catalog.service.SubscriptionService;
import com.codeman.platform.rbac.service.RequirePerm;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlanController {

    private final SubscriptionService service;

    public PlanController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/plans")
    @RequirePerm("plan:view")
    public List<PlanView> plans() {
        return service.plans();
    }

    @GetMapping("/subscriptions")
    @RequirePerm("plan:view")
    public List<SubscriptionView> subscriptions() {
        return service.list();
    }

    /** 创建订阅 → 自动据套餐签发 License。 */
    @PostMapping("/subscriptions")
    @RequirePerm("plan:subscribe")
    public SubscriptionView subscribe(@Valid @RequestBody CreateSubscriptionRequest req) {
        return service.create(req);
    }

    /** 套餐变更（升/降级）→ 重签关联 License。 */
    @PostMapping("/subscriptions/{id}/change")
    @RequirePerm("plan:subscribe")
    public SubscriptionView change(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return service.changePlan(id, body.get("planCode"));
    }

    /** 退订 → 吊销关联 License。 */
    @PostMapping("/subscriptions/{id}/cancel")
    @RequirePerm("plan:subscribe")
    public SubscriptionView cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
