package com.codeman.platform.cmprint;

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

/** CmPrint 商业授权:版本档位 / 签发(带能力微调) / 本产品 License 列表 / 审计查询。 */
@RestController
@RequestMapping("/api/cmprint")
public class CmprintController {

    private final CmprintService service;

    public CmprintController(CmprintService service) {
        this.service = service;
    }

    /** 三档版本预设与全键能力矩阵(供授权 UI 展示与签发表单初始化)。 */
    @GetMapping("/editions")
    @RequirePerm("cmprint:view")
    public CmprintDtos.EditionsView editions() {
        return service.editions();
    }

    @GetMapping("/licenses")
    @RequirePerm("cmprint:view")
    public List<LicenseView> licenses() {
        return service.licenses();
    }

    /** 签发 CmPrint License(续期/变更/吊销/下载复用通用 /api/licenses 端点)。 */
    @PostMapping("/licenses/issue")
    @RequirePerm("cmprint:issue")
    public LicenseDetail issue(@Valid @RequestBody CmprintDtos.IssueRequest req) {
        return service.issue(req);
    }

    @GetMapping("/audit")
    @RequirePerm("cmprint:audit")
    public CmprintDtos.AuditPage audit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.auditQuery(action, keyword, from, to, page, size);
    }

    @GetMapping("/audit/export.csv")
    @RequirePerm("cmprint:audit")
    public ResponseEntity<byte[]> auditCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var rows = service.auditQuery(action, keyword, from, to, 0, 500).rows().stream()
                .map(a -> List.<Object>of(a.createdAt(), a.actor(), a.action(),
                        a.tenantId() == null ? "" : a.tenantId(), a.detail() == null ? "" : a.detail()))
                .toList();
        return CsvUtil.response("cmprint-audit.csv",
                List.of("createdAt", "actor", "action", "tenantId", "detail"), rows);
    }
}
