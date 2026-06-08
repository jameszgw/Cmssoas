package com.codeman.platform.catalog.dto;

import com.codeman.platform.catalog.domain.Plan;
import com.codeman.platform.catalog.domain.Subscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class CatalogDtos {
    private CatalogDtos() {}

    public record FeatureView(String code, String name) {}
    public record ModuleView(String code, String name, List<FeatureView> features) {}
    public record ProductView(String code, String name, List<ModuleView> modules) {}

    public record MatrixRow(String module, String feature, String code, List<Boolean> avail) {}
    public record MatrixView(List<String> versions, List<MatrixRow> rows) {}

    public record PlanView(String code, String name, String planKey, int price,
                           String versionRange, int seats, List<String> modules, String status) {
        public static PlanView from(Plan p) {
            return new PlanView(p.getCode(), p.getName(), p.getPlanKey(), p.getPrice(),
                    p.getVersionRange(), p.getSeats(),
                    p.getModules().isBlank() ? List.of() : List.of(p.getModules().split(",")),
                    p.getStatus());
        }
    }

    public record CreateSubscriptionRequest(
            @NotBlank String tenantCode,
            @NotBlank String customer,
            @NotBlank String planCode,
            Integer qty,
            @NotNull LocalDate startAt,
            @NotNull LocalDate endAt
    ) {}

    public record SubscriptionView(
            Long id, String tenantCode, String customer, String planCode, int qty,
            String startAt, String endAt, String status, String licenseId
    ) {
        public static SubscriptionView from(Subscription s) {
            return new SubscriptionView(s.getId(), s.getTenantCode(), s.getCustomer(), s.getPlanCode(),
                    s.getQty(), s.getStartAt().toString(), s.getEndAt().toString(), s.getStatus(), s.getLicenseId());
        }
    }
}
