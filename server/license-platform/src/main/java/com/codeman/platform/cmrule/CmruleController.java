package com.codeman.platform.cmrule;

import com.codeman.platform.common.CsvUtil;
import com.codeman.platform.license.dto.LicenseDtos.LicenseDetail;
import com.codeman.platform.license.dto.LicenseDtos.LicenseView;
import com.codeman.platform.rbac.service.RequirePerm;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** CmRuleEngine 商业授权:版本档位 / 签发(带能力微调) / 本产品 License 列表 / 审计查询。 */
@RestController
@RequestMapping("/api/cmrule")
public class CmruleController {

    private final CmruleService service;

    public CmruleController(CmruleService service) {
        this.service = service;
    }

    /** 四档版本预设与全键能力矩阵(供授权 UI 展示与签发表单初始化)。 */
    @GetMapping("/editions")
    @RequirePerm("cmrule:view")
    public CmruleDtos.EditionsView editions() {
        return service.editions();
    }

    @GetMapping("/licenses")
    @RequirePerm("cmrule:view")
    public List<LicenseView> licenses() {
        return service.licenses();
    }

    /** 签发 CmRuleEngine License(续期/变更/吊销/下载复用通用 /api/licenses 端点)。 */
    @PostMapping("/licenses/issue")
    @RequirePerm("cmrule:issue")
    public LicenseDetail issue(@Valid @RequestBody CmruleDtos.IssueRequest req) {
        return service.issue(req);
    }

    @GetMapping("/audit")
    @RequirePerm("cmrule:audit")
    public CmruleDtos.AuditPage audit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.auditQuery(action, keyword, from, to, page, size);
    }

    @GetMapping("/audit/export.csv")
    @RequirePerm("cmrule:audit")
    public ResponseEntity<byte[]> auditCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var rows = service.auditQuery(action, keyword, from, to, 0, 500).rows().stream()
                .map(a -> List.<Object>of(a.createdAt(), a.actor(), a.action(),
                        a.tenantId() == null ? "" : a.tenantId(), a.detail() == null ? "" : a.detail()))
                .toList();
        return CsvUtil.response("cmrule-audit.csv",
                List.of("createdAt", "actor", "action", "tenantId", "detail"), rows);
    }
}
