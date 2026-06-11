package com.codeman.platform.cmrule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** CmRuleEngine 授权集成的请求/响应 DTO。 */
public final class CmruleDtos {
    private CmruleDtos() {}

    /** 单个版本档位:preset=相对全开被关的键;capabilities=解析后的全键布尔表(供 UI 矩阵展示)。 */
    public record EditionView(String edition, Map<String, Boolean> preset, Map<String, Boolean> capabilities) {}

    public record EditionsView(String productCode, String defaultVersionRange,
                               List<String> capabilityKeys, List<EditionView> editions) {}

    /**
     * 签发请求:edition 必填(COMMUNITY/PROFESSIONAL/ENTERPRISE/ULTIMATE);
     * overrides 为合同微调(能力键 → 布尔),仅接受 CmRuleEngine 能力键。
     */
    public record IssueRequest(
            @NotBlank String tenantCode,
            @NotBlank String customer,
            @NotBlank String edition,
            String mode,
            Map<String, Boolean> overrides,
            String appVersionRange,
            @NotNull LocalDate notBefore,
            @NotNull LocalDate notAfter,
            Integer concurrency,
            String reason
    ) {}

    public record AuditRow(Long id, String actor, String action, String detail, Long tenantId, String createdAt) {}

    public record AuditPage(long total, List<AuditRow> rows) {}
}
