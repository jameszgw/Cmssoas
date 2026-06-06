package com.cmssoas.platform.license.web;

import com.cmssoas.platform.license.dto.LicenseDtos.*;
import com.cmssoas.platform.license.service.LicenseService;
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

    public LicenseController(LicenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<LicenseView> list() {
        return service.list();
    }

    @GetMapping("/public-key")
    public PublicKeyView publicKey() {
        return service.publicKey();
    }

    @GetMapping("/crl")
    public List<String> crl() {
        return service.crl();
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
    public LicenseDetail issue(@Valid @RequestBody IssueRequest req) {
        return service.issue(req);
    }

    @PostMapping("/{licenseId}/renew")
    public LicenseDetail renew(@PathVariable String licenseId, @Valid @RequestBody RenewRequest req) {
        return service.renew(licenseId, req);
    }

    @PostMapping("/{licenseId}/modify")
    public LicenseDetail modify(@PathVariable String licenseId, @RequestBody ModifyRequest req) {
        return service.modify(licenseId, req);
    }

    @PostMapping("/{licenseId}/revoke")
    public LicenseDetail revoke(@PathVariable String licenseId, @RequestBody(required = false) RevokeRequest req) {
        return service.revoke(licenseId, req);
    }

    @PostMapping("/verify")
    public VerifyResult verify(@Valid @RequestBody VerifyRequest req) {
        return service.verify(req.lic());
    }
}
