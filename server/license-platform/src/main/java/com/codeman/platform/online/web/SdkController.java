package com.codeman.platform.online.web;

import com.codeman.platform.online.dto.OnlineDtos.*;
import com.codeman.platform.online.service.OnlineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** SDK 通道：客户端被保护应用调用（生产应启用 mTLS + 请求签名）。 */
@RestController
@RequestMapping("/sdk")
public class SdkController {

    private final OnlineService service;

    public SdkController(OnlineService service) {
        this.service = service;
    }

    @PostMapping("/activate")
    public ActivateResponse activate(@Valid @RequestBody ActivateRequest req, HttpServletRequest http) {
        return service.activate(req, http.getRemoteAddr());
    }

    @PostMapping("/heartbeat")
    public HeartbeatResponse heartbeat(@Valid @RequestBody HeartbeatRequest req) {
        return service.heartbeat(req);
    }

    @PostMapping("/deactivate")
    public Map<String, Object> deactivate(@Valid @RequestBody DeactivateRequest req) {
        service.deactivate(req);
        return Map.of("ok", true);
    }
}
