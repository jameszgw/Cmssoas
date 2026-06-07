package com.codeman.platform.license.service;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.license.domain.License;
import com.codeman.platform.license.domain.LicenseHistory;
import com.codeman.platform.license.domain.LicenseStatus;
import com.codeman.platform.license.dto.LicenseDtos.*;
import com.codeman.platform.license.repo.LicenseHistoryRepository;
import com.codeman.platform.license.repo.LicenseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64UD = Base64.getUrlDecoder();

    private final LicenseRepository licenseRepo;
    private final LicenseHistoryRepository historyRepo;
    private final SignatureService keyService;   // 可插拔签名实现（Ed25519 / SM2）
    private final ObjectMapper mapper;
    private final AuditWriter audit;

    public LicenseService(LicenseRepository licenseRepo, LicenseHistoryRepository historyRepo,
                          SignatureService keyService, ObjectMapper mapper, AuditWriter audit) {
        this.licenseRepo = licenseRepo;
        this.historyRepo = historyRepo;
        this.keyService = keyService;
        this.mapper = mapper;
        this.audit = audit;
    }

    // ---------- 查询 ----------
    public List<LicenseView> list() {
        return licenseRepo.findAllByOrderByCreatedAtDesc().stream().map(LicenseView::from).toList();
    }

    public LicenseDetail detail(String licenseId) {
        return LicenseDetail.from(require(licenseId));
    }

    public List<HistoryView> history(String licenseId) {
        return historyRepo.findByLicenseIdOrderByVersionDesc(licenseId).stream().map(HistoryView::from).toList();
    }

    public String downloadLic(String licenseId) {
        return require(licenseId).getLic();
    }

    public PublicKeyView publicKey() {
        return new PublicKeyView(keyService.algorithm(), keyService.publicKeyBase64());
    }

    /** JWKS 风格的公钥集（支持多 kid 轮换；当前为活跃密钥，轮换后保留旧公钥用于验旧 License）。 */
    public List<Map<String, String>> publicKeys() {
        return List.of(Map.of(
                "kid", keyService.kid(),
                "algorithm", keyService.algorithm(),
                "publicKeyBase64", keyService.publicKeyBase64()));
    }

    public List<String> crl() {
        return licenseRepo.findByStatus(LicenseStatus.REVOKED).stream().map(License::getLicenseId).toList();
    }

    /**
     * 已签名的吊销名单(CRL),供离线客户端拉取并用公钥(按 kid)验签。
     * 结构:{issuedAt, kid, sigAlg, count, revoked:[{licenseId, revokedAt}], payloadB64, signature}。
     * 离线 SDK:base64url 解码 payloadB64 → 用对应 kid 公钥 verify(signature) → 信任后据 revoked 拒绝已吊销 License。
     */
    public Map<String, Object> signedCrl() {
        List<Map<String, Object>> revoked = licenseRepo.findByStatus(LicenseStatus.REVOKED).stream()
                .map(l -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("licenseId", l.getLicenseId());
                    m.put("revokedAt", l.getUpdatedAt() == null ? null : l.getUpdatedAt().toString());
                    return m;
                }).toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issuedAt", LocalDateTime.now().toString());
        body.put("kid", keyService.kid());
        body.put("sigAlg", keyService.algorithm());
        body.put("count", revoked.size());
        body.put("revoked", revoked);

        byte[] payloadBytes = writeJson(body).getBytes(StandardCharsets.UTF_8);
        byte[] sig = keyService.sign(payloadBytes);
        Map<String, Object> out = new LinkedHashMap<>(body);
        out.put("payloadB64", B64U.encodeToString(payloadBytes));
        out.put("signature", B64U.encodeToString(sig));
        return out;
    }

    /** 到期自动停用:将 notAfter 已过的 ACTIVE License 置 EXPIRED 并重签(状态进入 .lic 与 CRL 逻辑)。 */
    @Transactional
    public int autoExpire() {
        int n = 0;
        for (License l : licenseRepo.findByStatus(LicenseStatus.ACTIVE)) {
            if (l.getNotAfter().isBefore(LocalDate.now())) {
                l.setStatus(LicenseStatus.EXPIRED);
                bumpAndResign(l, "EXPIRE", "auto-expire: 已过 notAfter");
                audit.log(null, "LICENSE_EXPIRED", l.getLicenseId() + " 到期自动停用");
                n++;
            }
        }
        if (n > 0) log.info("[license] 到期自动停用 {} 张", n);
        return n;
    }

    /** 每日自动到期检查。 */
    @Scheduled(fixedDelayString = "${app.license.auto-expire-interval-ms:86400000}", initialDelay = 90000)
    public void scheduledAutoExpire() {
        autoExpire();
    }

    // ---------- 签发 ----------
    @Transactional
    public LicenseDetail issue(IssueRequest r) {
        if (!r.notAfter().isAfter(r.notBefore())) {
            throw ApiException.badRequest("到期日必须晚于生效日");
        }
        License l = new License();
        l.setLicenseId(nextLicenseId());
        l.setTenantCode(r.tenantCode());
        l.setCustomer(r.customer());
        l.setProductCode(orElse(r.productCode(), "CODEMAN"));
        l.setEdition(orElse(r.edition(), "ENTERPRISE"));
        l.setMode(orElse(r.mode(), "HYBRID"));
        l.setModules(String.join(",", r.modules()));
        l.setFeatures(writeJson(r.features() == null ? Map.of() : r.features()));
        l.setAppVersionRange(orElse(r.appVersionRange(), ">=2.0.0 <3.0.0"));
        l.setNotBefore(r.notBefore());
        l.setNotAfter(r.notAfter());
        l.setConcurrency(r.concurrency() == null ? 1 : r.concurrency());
        l.setWatermark("WM-" + randomHex(8));
        l.setStatus(LicenseStatus.ACTIVE);
        l.setCurrentVersion(1);
        l.setCreatedAt(LocalDateTime.now());
        l.setUpdatedAt(LocalDateTime.now());
        resign(l);
        licenseRepo.save(l);
        historyRepo.save(LicenseHistory.of(l, "ISSUE", "operator", r.reason()));
        audit.log(null, "LICENSE_ISSUE", l.getLicenseId() + " · " + l.getEdition() + " · " + l.getCustomer());
        log.info("[license] 已签发 {} v{}", l.getLicenseId(), l.getCurrentVersion());
        return LicenseDetail.from(l);
    }

    // ---------- 续期 ----------
    @Transactional
    public LicenseDetail renew(String licenseId, RenewRequest r) {
        License l = require(licenseId);
        if (l.getStatus() == LicenseStatus.REVOKED) throw ApiException.badRequest("已吊销的 License 不可续期");
        if (!r.notAfter().isAfter(l.getNotBefore())) throw ApiException.badRequest("到期日必须晚于生效日");
        l.setNotAfter(r.notAfter());
        if (l.getStatus() == LicenseStatus.EXPIRED) l.setStatus(LicenseStatus.ACTIVE);
        bumpAndResign(l, "RENEW", r.reason());
        audit.log(null, "LICENSE_RENEW", l.getLicenseId() + " -> " + l.getNotAfter() + " v" + l.getCurrentVersion());
        return LicenseDetail.from(l);
    }

    // ---------- 变更 ----------
    @Transactional
    public LicenseDetail modify(String licenseId, ModifyRequest r) {
        License l = require(licenseId);
        if (l.getStatus() == LicenseStatus.REVOKED) throw ApiException.badRequest("已吊销的 License 不可变更");
        if (r.modules() != null) l.setModules(String.join(",", r.modules()));
        if (r.features() != null) l.setFeatures(writeJson(r.features()));
        if (r.appVersionRange() != null && !r.appVersionRange().isBlank()) l.setAppVersionRange(r.appVersionRange());
        if (r.concurrency() != null) l.setConcurrency(r.concurrency());
        if (r.edition() != null && !r.edition().isBlank()) l.setEdition(r.edition());
        bumpAndResign(l, "MODIFY", r.reason());
        audit.log(null, "LICENSE_MODIFY", l.getLicenseId() + " v" + l.getCurrentVersion());
        return LicenseDetail.from(l);
    }

    // ---------- 吊销 ----------
    @Transactional
    public LicenseDetail revoke(String licenseId, RevokeRequest r) {
        License l = require(licenseId);
        if (l.getStatus() == LicenseStatus.REVOKED) throw ApiException.badRequest("该 License 已吊销");
        l.setStatus(LicenseStatus.REVOKED);
        bumpAndResign(l, "REVOKE", r == null ? null : r.reason());
        audit.log(null, "LICENSE_REVOKE", l.getLicenseId() + " · " + (r == null ? "" : r.reason()));
        log.info("[license] 已吊销 {}", licenseId);
        return LicenseDetail.from(l);
    }

    // ---------- 验签（模拟 SDK 校验）----------
    public VerifyResult verify(String lic) {
        try {
            String[] parts = lic.trim().split("\\.");
            if (parts.length != 2) return new VerifyResult(false, "格式错误", null);
            byte[] payload = B64UD.decode(parts[0]);
            byte[] sig = B64UD.decode(parts[1]);
            if (!keyService.verify(payload, sig)) {
                return new VerifyResult(false, "签名无效（伪造或被篡改）", null);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = mapper.readValue(payload, Map.class);
            String licenseId = String.valueOf(claims.get("licenseId"));
            // CRL 检查
            License l = licenseRepo.findByLicenseId(licenseId).orElse(null);
            if (l != null && l.getStatus() == LicenseStatus.REVOKED) {
                return new VerifyResult(false, "License 已被吊销", claims);
            }
            // 有效期检查
            Object na = claims.get("notAfter");
            if (na != null && LocalDate.parse(na.toString()).isBefore(LocalDate.now())) {
                return new VerifyResult(false, "License 已过期", claims);
            }
            return new VerifyResult(true, "有效", claims);
        } catch (Exception e) {
            return new VerifyResult(false, "解析失败：" + e.getMessage(), null);
        }
    }

    // ---------- 内部 ----------
    private void bumpAndResign(License l, String op, String reason) {
        l.setCurrentVersion(l.getCurrentVersion() + 1);
        l.setUpdatedAt(LocalDateTime.now());
        resign(l);
        licenseRepo.save(l);
        historyRepo.save(LicenseHistory.of(l, op, "operator", reason));
    }

    /** 依据实体当前字段构建 claims、签名并写回（claimsJson / signature / lic）。 */
    private void resign(License l) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("schemaVersion", 2);
        claims.put("licenseId", l.getLicenseId());
        claims.put("licenseVersion", l.getCurrentVersion());
        claims.put("tenantCode", l.getTenantCode());
        claims.put("customer", l.getCustomer());
        claims.put("productCode", l.getProductCode());
        claims.put("edition", l.getEdition());
        claims.put("mode", l.getMode());
        claims.put("modules", l.getModules().isBlank() ? List.of() : Arrays.asList(l.getModules().split(",")));
        claims.put("features", readJson(l.getFeatures()));
        claims.put("appVersionRange", l.getAppVersionRange());
        claims.put("notBefore", l.getNotBefore().toString());
        claims.put("notAfter", l.getNotAfter().toString());
        claims.put("concurrency", l.getConcurrency());
        claims.put("status", l.getStatus().name());
        claims.put("watermark", l.getWatermark());
        claims.put("sigAlg", keyService.algorithm());   // 签名算法（Ed25519 / SM2），供 SDK 选择验签
        claims.put("kid", keyService.kid());             // 密钥标识，支持多公钥轮换
        claims.put("issuedAt", LocalDateTime.now().toString());

        String payload = writeJson(claims);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] sig = keyService.sign(payloadBytes);
        l.setClaimsJson(payload);
        l.setSignature(B64U.encodeToString(sig));
        l.setLic(B64U.encodeToString(payloadBytes) + "." + B64U.encodeToString(sig));
    }

    private License require(String licenseId) {
        return licenseRepo.findByLicenseId(licenseId)
                .orElseThrow(() -> ApiException.notFound("License 不存在：" + licenseId));
    }

    private String nextLicenseId() {
        int year = Year.now().getValue();
        long seq = licenseRepo.count() + 1;
        String id;
        do {
            id = String.format("LIC-%d-%04d", year, seq);
            seq++;
        } while (licenseRepo.findByLicenseId(id).isPresent());
        return id;
    }

    private String writeJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { throw new IllegalStateException("JSON 序列化失败", e); }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String s) {
        try { return s == null || s.isBlank() ? new LinkedHashMap<>() : mapper.readValue(s, LinkedHashMap.class); }
        catch (Exception e) { return new LinkedHashMap<>(); }
    }

    private static String orElse(String v, String def) { return (v == null || v.isBlank()) ? def : v; }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
