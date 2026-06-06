package com.cmssoas.platform.tenant.web;

import com.cmssoas.platform.tenant.dto.ActivateRequest;
import com.cmssoas.platform.tenant.dto.ActivationInfo;
import com.cmssoas.platform.tenant.service.ActivationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activation")
public class ActivationController {

    private final ActivationService activationService;

    public ActivationController(ActivationService activationService) {
        this.activationService = activationService;
    }

    /** 激活页：凭 token 获取租户/管理员信息。 */
    @GetMapping("/{token}")
    public ActivationInfo info(@PathVariable String token) {
        return activationService.info(token);
    }

    /** 提交激活：设置管理员密码。 */
    @PostMapping("/{token}")
    public Map<String, Object> activate(@PathVariable String token, @Valid @RequestBody ActivateRequest req) {
        activationService.activate(token, req.password(), req.mfaCode());
        return Map.of("success", true, "message", "账户已激活，请使用新密码登录");
    }
}
