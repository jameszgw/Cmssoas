package com.cmssoas.platform.overview;

import com.cmssoas.platform.catalog.repo.SubscriptionRepository;
import com.cmssoas.platform.license.domain.License;
import com.cmssoas.platform.license.domain.LicenseStatus;
import com.cmssoas.platform.license.repo.LicenseRepository;
import com.cmssoas.platform.online.service.OnlineService;
import com.cmssoas.platform.rbac.service.RequirePerm;
import com.cmssoas.platform.tenant.repo.TenantRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

/**
 * 运营总览：真实统计 + 主动告警（到期预警 / 在线异常）。
 * 替代前端总览页的示例数据。
 */
@RestController
@RequestMapping("/api/overview")
public class OverviewController {

    private static final int EXPIRE_SOON_DAYS = 30;

    private final LicenseRepository licenseRepo;
    private final TenantRepository tenantRepo;
    private final SubscriptionRepository subRepo;
    private final OnlineService onlineService;

    public OverviewController(LicenseRepository licenseRepo, TenantRepository tenantRepo,
                             SubscriptionRepository subRepo, OnlineService onlineService) {
        this.licenseRepo = licenseRepo;
        this.tenantRepo = tenantRepo;
        this.subRepo = subRepo;
        this.onlineService = onlineService;
    }

    /** 实时统计 KPI。 */
    @GetMapping("/stats")
    @RequirePerm("overview")
    public Map<String, Object> stats() {
        List<License> all = licenseRepo.findAll();
        LocalDate now = LocalDate.now();
        long active = all.stream().filter(l -> l.getStatus() == LicenseStatus.ACTIVE).count();
        long revoked = all.stream().filter(l -> l.getStatus() == LicenseStatus.REVOKED).count();
        long expireSoon = all.stream().filter(l -> l.getStatus() == LicenseStatus.ACTIVE
                && !l.getNotAfter().isBefore(now) && l.getNotAfter().isBefore(now.plusDays(EXPIRE_SOON_DAYS))).count();
        var online = onlineService.stats();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("licensesTotal", all.size());
        m.put("licensesActive", active);
        m.put("licensesExpireSoon", expireSoon);
        m.put("licensesRevoked", revoked);
        m.put("tenants", tenantRepo.count());
        m.put("subscriptions", subRepo.count());
        m.put("onlineInstances", online.onlineInstances());
        m.put("graceInstances", online.graceInstances());
        m.put("seatsUsed", online.totalSeatsUsed());
        return m;
    }

    /** 运营待办 / 主动告警：到期预警、已过期未处理、在线异常。 */
    @GetMapping("/alerts")
    @RequirePerm("overview")
    public List<Map<String, Object>> alerts() {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (License l : licenseRepo.findByStatus(LicenseStatus.ACTIVE)) {
            long days = now.until(l.getNotAfter()).getDays();
            if (l.getNotAfter().isBefore(now)) {
                out.add(alert("EXPIRED", "danger", l.getLicenseId() + " · " + l.getCustomer() + " 已过期未处理", l.getLicenseId()));
            } else if (l.getNotAfter().isBefore(now.plusDays(EXPIRE_SOON_DAYS))) {
                out.add(alert("EXPIRE_SOON", "warning",
                        l.getLicenseId() + " · " + l.getCustomer() + " 将于 " + days + " 天后到期", l.getLicenseId()));
            }
        }
        var st = onlineService.stats();
        if (st.graceInstances() > 0) {
            out.add(alert("HEARTBEAT", "warning", "有 " + st.graceInstances() + " 个在线实例心跳异常（宽限期）", null));
        }
        return out;
    }

    private Map<String, Object> alert(String type, String level, String message, String ref) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type); m.put("level", level); m.put("message", message); m.put("ref", ref);
        return m;
    }
}
