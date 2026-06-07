package com.cmssoas.platform.license.web;

import com.cmssoas.platform.license.dto.LicenseDtos.*;
import com.cmssoas.platform.license.service.LicenseService;
import com.cmssoas.platform.license.service.LicenseExpiryReminder;
import com.cmssoas.platform.rbac.service.RequirePerm;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService service;
    private final LicenseExpiryReminder expiryReminder;

    public LicenseController(LicenseService service, LicenseExpiryReminder expiryReminder) {
        this.service = service;
        this.expiryReminder = expiryReminder;
    }

    @GetMapping
    @RequirePerm("license:view")
    public List<LicenseView> list() {
        return service.list();
    }

    @GetMapping("/public-key")
    public PublicKeyView publicKey() {
        return service.publicKey();
    }

    /** JWKS 风格公钥集（含 kid），SDK 可按 License 的 kid 选公钥验签，支持密钥轮换。 */
    @GetMapping("/public-keys")
    public List<java.util.Map<String, String>> publicKeys() {
        return service.publicKeys();
    }

    @GetMapping("/crl")
    public List<String> crl() {
        return service.crl();
    }

    /** 导出 License 列表为 CSV。 */
    @GetMapping("/export.csv")
    @RequirePerm("license:view")
    public org.springframework.http.ResponseEntity<byte[]> exportCsv() {
        var rows = service.list().stream().map(l -> java.util.List.<Object>of(
                l.licenseId(), l.tenantCode(), l.customer(), l.edition(), l.status(),
                l.notAfter(), l.version(), l.appVersionRange())).toList();
        return com.cmssoas.platform.common.CsvUtil.response("licenses.csv",
                java.util.List.of("licenseId", "tenantCode", "customer", "edition", "status", "notAfter", "version", "appVersionRange"),
                rows);
    }

    /** 手动触发到期提醒扫描（经发件箱外发邮件），返回本次发送数。 */
    @PostMapping("/run-expiry-reminder")
    @RequirePerm("license:view")
    public java.util.Map<String, Object> runExpiryReminder() {
        return java.util.Map.of("sent", expiryReminder.runOnce());
    }

    @GetMapping("/{licenseId}")
    public LicenseDetail detail(@PathVariable String licenseId) {
        return service.detail(licenseId);
    }

    @GetMapping("/{licenseId}/history")
    public List<HistoryView> history(@PathVariable String licenseId) {
        return service.history(licenseId);
    }

    @GetMapping("/{licenseId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String licenseId) {
        byte[] body = service.downloadLic(licenseId).getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + licenseId + ".lic\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @PostMapping("/issue")
    @RequirePerm("license:issue")
    public LicenseDetail issue(@Valid @RequestBody IssueRequest req) {
        return service.issue(req);
    }

    @PostMapping("/{licenseId}/renew")
    @RequirePerm("license:renew")
    public LicenseDetail renew(@PathVariable String licenseId, @Valid @RequestBody RenewRequest req) {
        return service.renew(licenseId, req);
    }

    @PostMapping("/{licenseId}/modify")
    @RequirePerm("license:issue")
    public LicenseDetail modify(@PathVariable String licenseId, @RequestBody ModifyRequest req) {
        return service.modify(licenseId, req);
    }

    @PostMapping("/{licenseId}/revoke")
    @RequirePerm("license:revoke")
    public LicenseDetail revoke(@PathVariable String licenseId, @RequestBody(required = false) RevokeRequest req) {
        return service.revoke(licenseId, req);
    }

    @PostMapping("/verify")
    public VerifyResult verify(@Valid @RequestBody VerifyRequest req) {
        return service.verify(req.lic());
    }
}
