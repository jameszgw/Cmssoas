package com.codeman.platform.license.web;

import com.codeman.platform.license.service.LicenseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * License 公开分发(离线客户端用,无需登录)。路径不在 /api/** 下,JwtAuthFilter 放行。
 * - 公钥集(JWKS 风格):离线 SDK 按 kid 选公钥验签 .lic 与 CRL。
 * - 已签名吊销名单(CRL):离线 SDK 定期拉取并验签,据此拒绝已吊销 License。
 */
@RestController
@RequestMapping("/pub")
public class PublicLicenseController {

    private final LicenseService service;

    public PublicLicenseController(LicenseService service) {
        this.service = service;
    }

    @GetMapping("/license/public-keys")
    public List<Map<String, String>> publicKeys() {
        return service.publicKeys();
    }

    @GetMapping("/crl")
    public Map<String, Object> crl() {
        return service.signedCrl();
    }
}
