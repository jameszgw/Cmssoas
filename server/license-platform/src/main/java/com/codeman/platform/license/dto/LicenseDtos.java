package com.codeman.platform.license.dto;

import com.codeman.platform.license.domain.License;
import com.codeman.platform.license.domain.LicenseHistory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** License 模块的请求/响应 DTO 集合。 */
public final class LicenseDtos {
    private LicenseDtos() {}

    public record IssueRequest(
            @NotBlank String tenantCode,
            @NotBlank String customer,
            String productCode,
            String edition,
            String mode,
            @NotNull List<String> modules,
            Map<String, Object> features,
            String appVersionRange,
            @NotNull LocalDate notBefore,
            @NotNull LocalDate notAfter,
            Integer concurrency,
            String reason
    ) {}

    public record RenewRequest(@NotNull LocalDate notAfter, String reason) {}

    public record ModifyRequest(
            List<String> modules,
            Map<String, Object> features,
            String appVersionRange,
            Integer concurrency,
            String edition,
            String reason
    ) {}

    public record RevokeRequest(String reason) {}

    public record VerifyRequest(@NotBlank String lic) {}

    public record VerifyResult(boolean valid, String reason, Map<String, Object> claims) {}

    public record PublicKeyView(String algorithm, String publicKeyBase64) {}

    public record LicenseView(
            String licenseId, String tenantCode, String customer, String edition,
            String mode, String status, int version, String appVersionRange,
            String notAfter, List<String> modules
    ) {
        public static LicenseView from(License l) {
            return new LicenseView(
                    l.getLicenseId(), l.getTenantCode(), l.getCustomer(), l.getEdition(),
                    l.getMode(), l.getStatus().name(), l.getCurrentVersion(), l.getAppVersionRange(),
                    l.getNotAfter().toString(),
                    l.getModules().isBlank() ? List.of() : Arrays.asList(l.getModules().split(","))
            );
        }
    }

    public record LicenseDetail(
            String licenseId, String tenantCode, String customer, String productCode,
            String edition, String mode, String status, int version, String appVersionRange,
            String notBefore, String notAfter, int concurrency, String watermark,
            List<String> modules, String claimsJson, String signature
    ) {
        public static LicenseDetail from(License l) {
            return new LicenseDetail(
                    l.getLicenseId(), l.getTenantCode(), l.getCustomer(), l.getProductCode(),
                    l.getEdition(), l.getMode(), l.getStatus().name(), l.getCurrentVersion(), l.getAppVersionRange(),
                    l.getNotBefore().toString(), l.getNotAfter().toString(), l.getConcurrency(), l.getWatermark(),
                    l.getModules().isBlank() ? List.of() : Arrays.asList(l.getModules().split(",")),
                    l.getClaimsJson(), l.getSignature()
            );
        }
    }

    public record HistoryView(
            int version, String opType, String operator, String reason,
            String createdAt, String claimsJson
    ) {
        public static HistoryView from(LicenseHistory h) {
            return new HistoryView(h.getVersion(), h.getOpType(), h.getOperator(), h.getReason(),
                    h.getCreatedAt().toString(), h.getClaimsJson());
        }
    }
}
