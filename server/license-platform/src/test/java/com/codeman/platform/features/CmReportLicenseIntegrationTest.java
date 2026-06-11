package com.codeman.platform.features;

import com.codeman.platform.cmreport.CmReportKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端:CmReport 产品线授权集成。
 * 覆盖:版本矩阵下发、按版本手工签发(RSA SHA256 产品格式:payload 字段/附加包/限额/指纹)、
 * 公钥分发并以「产品侧同款验签方式」核验令牌、续期重签仍为产品格式、
 * CMREPORT-* 套餐订阅自动签发路由、未知版本/附加包拒绝。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CmReportLicenseIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired CmReportKeyService keyService;
    @Autowired ObjectMapper mapper;
    private HttpHeaders auth;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        ResponseEntity<Map> r = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "8888"), Map.class);
        auth = new HttpHeaders();
        auth.setBearerAuth((String) r.getBody().get("token"));
        auth.setContentType(MediaType.APPLICATION_JSON);
    }

    private <T> ResponseEntity<T> post(String url, Object body, Class<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, auth), type);
    }

    private <T> ResponseEntity<T> get(String url, Class<T> type) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(auth), type);
    }

    @Test
    @SuppressWarnings("unchecked")
    void editionsMatrix() {
        Map<String, Object> m = get("/api/cmreport/editions", Map.class).getBody();
        assertEquals(List.of("community", "lite", "pro", "enterprise", "ultimate"), m.get("editions"));
        Map<String, List<String>> matrix = (Map<String, List<String>>) m.get("matrix");
        assertTrue(matrix.get("pro").contains("dataset.query"), "pro 应含取数端点能力");
        assertTrue(matrix.get("enterprise").contains("perm.rbac.rowcol.audit"), "enterprise 应含行列权限审计");
        assertFalse(matrix.get("lite").contains("dashboard"), "lite 不应含大屏");
        assertTrue(matrix.get("ultimate").containsAll(matrix.get("enterprise")), "版本能力应累进");
    }

    @Test
    @SuppressWarnings("unchecked")
    void issueAndVerifyWithProductSemantics() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantCode", "T-CMR1", "customer", "测试客户A",
                "edition", "PRO",
                "addons", List.of("ai.pack"),
                "limits", Map.of("concurrency", 100, "nodes", 4),
                "fingerprint", "fp-abc",
                "notBefore", LocalDate.now().toString(),
                "notAfter", LocalDate.now().plusYears(1).toString());
        Map<String, Object> detail = post("/api/cmreport/licenses/issue", req, Map.class).getBody();
        assertEquals("CMREPORT", detail.get("productCode"));
        assertEquals("pro", detail.get("edition"));
        String licenseId = (String) detail.get("licenseId");

        // 下载 .lic(通用端点)并按「产品侧 LicenseSigner.verifyAndParse 同款」流程验签
        String lic = get("/api/licenses/" + licenseId + "/download", String.class).getBody();
        assertNotNull(lic);
        Map<String, Object> payload = verifyRsaAndParse(lic.trim());
        assertEquals(licenseId, payload.get("licenseId"));
        assertEquals("测试客户A", payload.get("customer"));
        assertEquals("pro", payload.get("edition"));
        assertEquals(List.of("ai.pack"), payload.get("capabilities"));
        assertEquals(List.of("ai.pack"), payload.get("addons"));
        Map<String, Object> limits = (Map<String, Object>) payload.get("limits");
        assertEquals(100, ((Number) limits.get("concurrency")).intValue());
        assertEquals(4, ((Number) limits.get("nodes")).intValue());
        assertEquals("fp-abc", payload.get("fingerprint"));
        assertTrue(((Number) payload.get("expiresAt")).longValue() > System.currentTimeMillis(), "应未过期");

        // 运营侧自检端点
        Map<String, Object> verify = get("/api/cmreport/licenses/" + licenseId + "/verify", Map.class).getBody();
        assertEquals(Boolean.TRUE, verify.get("valid"));

        // 续期:重签仍为产品格式(RSA 验签通过 + expiresAt 延长)
        long oldExpires = ((Number) payload.get("expiresAt")).longValue();
        post("/api/licenses/" + licenseId + "/renew",
                Map.of("notAfter", LocalDate.now().plusYears(2).toString(), "reason", "续期一年"), Map.class);
        String lic2 = get("/api/licenses/" + licenseId + "/download", String.class).getBody();
        Map<String, Object> payload2 = verifyRsaAndParse(lic2.trim());
        assertTrue(((Number) payload2.get("expiresAt")).longValue() > oldExpires, "续期后到期应延长");
        assertEquals("pro", payload2.get("edition"), "续期不改版本");

        // CmReport 产品线列表应包含
        List<Map<String, Object>> list = get("/api/cmreport/licenses", List.class).getBody();
        assertTrue(list.stream().anyMatch(x -> licenseId.equals(x.get("licenseId"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionAutoIssueRoutesToCmReport() throws Exception {
        Map<String, Object> req = Map.of(
                "tenantCode", "T-CMR2", "customer", "订阅客户B",
                "planCode", "CMREPORT-ENT", "qty", 1,
                "startAt", LocalDate.now().toString(),
                "endAt", LocalDate.now().plusYears(1).toString());
        Map<String, Object> sub = post("/api/subscriptions", req, Map.class).getBody();
        String licenseId = (String) sub.get("licenseId");
        assertNotNull(licenseId, "订阅应自动签发 License");

        String lic = get("/api/licenses/" + licenseId + "/download", String.class).getBody();
        Map<String, Object> payload = verifyRsaAndParse(lic.trim());
        assertEquals("enterprise", payload.get("edition"), "CMREPORT-ENT 套餐应签发 enterprise 版");
        Map<String, Object> limits = (Map<String, Object>) payload.get("limits");
        assertEquals(500, ((Number) limits.get("concurrency")).intValue(), "限额应取套餐 features.limits");
    }

    @Test
    void rejectUnknownEditionAndAddon() {
        Map<String, Object> bad1 = Map.of(
                "tenantCode", "T", "customer", "C", "edition", "mega",
                "notBefore", LocalDate.now().toString(), "notAfter", LocalDate.now().plusDays(1).toString());
        assertTrue(post("/api/cmreport/licenses/issue", bad1, String.class).getStatusCode().is4xxClientError());

        Map<String, Object> bad2 = Map.of(
                "tenantCode", "T", "customer", "C", "edition", "pro",
                "addons", List.of("not.a.capability"),
                "notBefore", LocalDate.now().toString(), "notAfter", LocalDate.now().plusDays(1).toString());
        assertTrue(post("/api/cmreport/licenses/issue", bad2, String.class).getStatusCode().is4xxClientError());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicKeyEndpoint() {
        Map<String, String> pk = get("/api/cmreport/public-key", Map.class).getBody();
        assertEquals("SHA256withRSA", pk.get("algorithm"));
        assertEquals(keyService.publicKeyBase64(), pk.get("publicKeyBase64"));
        assertEquals(16, pk.get("kid").length());
    }

    /** 与 CmReport 产品 LicenseSigner.verifyAndParse 等价的验签 + 解析(X509 公钥取自公钥端点)。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyRsaAndParse(String token) throws Exception {
        Map<String, String> pk = get("/api/cmreport/public-key", Map.class).getBody();
        PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(pk.get("publicKeyBase64"))));
        int dot = token.indexOf('.');
        assertTrue(dot > 0, "令牌应为 payload.signature 两段式");
        byte[] payload = Base64.getUrlDecoder().decode(token.substring(0, dot));
        byte[] sig = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(pub);
        s.update(payload);
        assertTrue(s.verify(sig), "RSA SHA256 验签应通过");
        return mapper.readValue(new String(payload, StandardCharsets.UTF_8), Map.class);
    }
}
