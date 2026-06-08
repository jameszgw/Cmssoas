package com.codeman.platform.portal.web;

import com.codeman.platform.portal.service.PortalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 租户自助门户(最终客户侧,公开)。路径不在 /api/** 下,JwtAuthFilter 放行;
 * 门户 token 在本控制器内自行校验(仅限本租户、只读)。
 */
@RestController
@RequestMapping("/pub/portal")
public class PublicPortalController {

    private final PortalService service;

    public PublicPortalController(PortalService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return service.login(body.get("tenantCode"), body.get("accessCode"));
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(HttpServletRequest req) {
        String tenantCode = service.tenantFromAuth(req.getHeader("Authorization"));
        return service.overview(tenantCode);
    }
}
