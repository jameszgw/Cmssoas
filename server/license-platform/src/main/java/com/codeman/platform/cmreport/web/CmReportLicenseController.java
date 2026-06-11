package com.codeman.platform.cmreport.web;

import com.codeman.platform.cmreport.CmReportEditions;
import com.codeman.platform.cmreport.CmReportKeyService;
import com.codeman.platform.cmreport.CmReportLicenseSigner;
import com.codeman.platform.common.ApiException;
import com.codeman.platform.license.domain.License;
import com.codeman.platform.license.dto.LicenseDtos.IssueRequest;
import com.codeman.platform.license.dto.LicenseDtos.LicenseDetail;
import com.codeman.platform.license.dto.LicenseDtos.LicenseView;
import com.codeman.platform.license.repo.LicenseRepository;
import com.codeman.platform.license.service.LicenseService;
import com.codeman.platform.rbac.service.RequirePerm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CmReport 产品线授权端点:版本矩阵下发、按版本签发(RSA SHA256,产品格式)、列表与公钥分发。
 * 续期/变更/吊销/下载/历史复用通用 /api/licenses/*(LicenseService 已按产品路由重签格式)。
 * 权限点复用 license:*(签发 license:issue、查看 license:view)。
 */
@RestController
@RequestMapping("/api/cmreport")
public class CmReportLicenseController {

    private final LicenseService licenseService;
    private final LicenseRepository licenseRepo;
    private final CmReportKeyService keyService;

    public CmReportLicenseController(LicenseService licenseService, LicenseRepository licenseRepo,
                                     CmReportKeyService keyService) {
        this.licenseService = licenseService;
        this.licenseRepo = licenseRepo;
        this.keyService = keyService;
    }

    /** CmReport 签发请求:版本 + 附加包 + 限额 + 指纹(产品 License 语义)。 */
    public record CmReportIssueRequest(
            @NotBlank String tenantCode,
            @NotBlank String customer,
            @NotBlank String edition,
            List<String> addons,
            Map<String, Long> limits,
            String fingerprint,
            @NotNull LocalDate notBefore,
            @NotNull LocalDate notAfter,
            String reason
    ) {}

    /** 版本矩阵(edition → 累计能力集)+ 附加包与限额键,供运营台 UI。 */
    @GetMapping("/editions")
    @RequirePerm("license:view")
    public Map<String, Object> editions() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("editions", CmReportEditions.EDITIONS);
        out.put("matrix", CmReportEditions.matrix());
        out.put("addons", CmReportEditions.ADDONS);
        out.put("limitKeys", CmReportEditions.LIMIT_KEYS);
        return out;
    }

    /** CmReport 产品线 License 列表。 */
    @GetMapping("/licenses")
    @RequirePerm("license:view")
    public List<LicenseView> list() {
        return licenseRepo.findByProductCodeOrderByCreatedAtDesc(CmReportLicenseSigner.PRODUCT_CODE)
                .stream().map(LicenseView::from).toList();
    }

    /**
     * 按版本签发 CmReport License(离线令牌)。
     * 附加信息(addons/limits/fingerprint)进 features JSON,由产品格式签发器读出落入 payload。
     */
    @PostMapping("/licenses/issue")
    @RequirePerm("license:issue")
    public LicenseDetail issue(@Valid @RequestBody CmReportIssueRequest r) {
        String edition = r.edition().trim().toLowerCase();
        if (!CmReportEditions.isEdition(edition)) {
            throw ApiException.badRequest("未知 CmReport 版本：" + r.edition()
                    + "(可选:" + String.join("/", CmReportEditions.EDITIONS) + ")");
        }
        if (r.addons() != null) {
            for (String a : r.addons()) {
                if (!CmReportEditions.ADDONS.contains(a)) {
                    throw ApiException.badRequest("未知附加包能力码：" + a);
                }
            }
        }
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("cmEdition", edition);
        features.put("addons", r.addons() == null ? List.of() : r.addons());
        features.put("limits", r.limits() == null ? Map.of() : r.limits());
        features.put("fingerprint", r.fingerprint() == null ? "" : r.fingerprint().trim());

        Long concurrency = r.limits() == null ? null : r.limits().get("concurrency");
        IssueRequest issue = new IssueRequest(
                r.tenantCode(), r.customer(), CmReportLicenseSigner.PRODUCT_CODE, edition, "OFFLINE",
                List.of(), features, ">=1.0.0 <2.0.0",
                r.notBefore(), r.notAfter(),
                concurrency == null ? 0 : concurrency.intValue(),
                r.reason() == null ? "CmReport 手工签发(" + edition + ")" : r.reason());
        return licenseService.issue(issue);
    }

    /** CmReport 验签公钥(X509 Base64),粘贴到产品 cmreport.license.public-key。 */
    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of(
                "algorithm", keyService.algorithm(),
                "kid", keyService.kid(),
                "publicKeyBase64", keyService.publicKeyBase64());
    }

    /** 单张授权的 CmReport 令牌校验(运营侧自检;校验 RSA 签名 + 解析 payload)。 */
    @GetMapping("/licenses/{licenseId}/verify")
    @RequirePerm("license:view")
    public Map<String, Object> verify(@org.springframework.web.bind.annotation.PathVariable String licenseId) {
        License l = licenseRepo.findByLicenseId(licenseId)
                .orElseThrow(() -> ApiException.notFound("License 不存在：" + licenseId));
        if (!CmReportLicenseSigner.isCmReport(l)) {
            throw ApiException.badRequest("非 CmReport 产品授权：" + licenseId);
        }
        String lic = l.getLic();
        int dot = lic.indexOf('.');
        byte[] payload = java.util.Base64.getUrlDecoder().decode(lic.substring(0, dot));
        byte[] sig = java.util.Base64.getUrlDecoder().decode(lic.substring(dot + 1));
        boolean valid = keyService.verify(payload, sig);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("valid", valid);
        out.put("payload", new String(payload, java.nio.charset.StandardCharsets.UTF_8));
        return out;
    }
}
