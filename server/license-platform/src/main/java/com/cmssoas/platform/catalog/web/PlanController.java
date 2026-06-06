package com.cmssoas.platform.catalog.web;

import com.cmssoas.platform.catalog.dto.CatalogDtos.*;
import com.cmssoas.platform.catalog.service.SubscriptionService;
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
    public List<PlanView> plans() {
        return service.plans();
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionView> subscriptions() {
        return service.list();
    }

    /** 创建订阅 → 自动据套餐签发 License。 */
    @PostMapping("/subscriptions")
    public SubscriptionView subscribe(@Valid @RequestBody CreateSubscriptionRequest req) {
        return service.create(req);
    }
}
