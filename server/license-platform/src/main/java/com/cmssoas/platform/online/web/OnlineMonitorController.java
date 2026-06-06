package com.cmssoas.platform.online.web;

import com.cmssoas.platform.online.dto.OnlineDtos.*;
import com.cmssoas.platform.online.service.OnlineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 运营后台：在线监控（实例 / 席位 / 心跳异常）。 */
@RestController
@RequestMapping("/api/online")
public class OnlineMonitorController {

    private final OnlineService service;

    public OnlineMonitorController(OnlineService service) {
        this.service = service;
    }

    @GetMapping("/instances")
    public List<InstanceView> instances() {
        return service.instances();
    }

    @GetMapping("/seats")
    public List<SeatUsage> seats() {
        return service.seatUsage();
    }

    @GetMapping("/stats")
    public OnlineStats stats() {
        return service.stats();
    }
}
