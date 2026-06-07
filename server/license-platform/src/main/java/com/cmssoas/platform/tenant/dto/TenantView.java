package com.cmssoas.platform.tenant.dto;

import com.cmssoas.platform.tenant.domain.Tenant;

/** 租户列表视图（与前端 Tenant 类型对应）。status 用小写以匹配前端样式键。 */
public record TenantView(
        String code,
        String name,
        String plan,      // 前端 i18n key，如 plan.ent
        String version,
        String status,    // active / soon / init / exp
        String expire,
        String email
) {
    public static TenantView from(Tenant t) {
        return new TenantView(
                t.getCode(),
                t.getName(),
                t.getPlanKey(),
                t.getVersion(),
                mapStatus(t),
                t.getExpireAt() == null ? "" : t.getExpireAt().toString(),
                t.getAdminEmail()
        );
    }

    private static String mapStatus(Tenant t) {
        return switch (t.getStatus()) {
            case INITIALIZING -> "init";
            case ACTIVE -> "active";
            case SUSPENDED -> "soon";
            case FAILED -> "exp";
        };
    }
}
