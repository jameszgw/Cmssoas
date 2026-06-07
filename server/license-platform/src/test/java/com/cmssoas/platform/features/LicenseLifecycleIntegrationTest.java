package com.cmssoas.platform.features;

import com.cmssoas.platform.license.service.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 端到端:License 吊销分发(签名 CRL,可公开拉取+验签)与到期自动停用。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LicenseLifecycleIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired SignatureService sig;
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

    private <T> T post(String url, Object body, Class<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, auth), type).getBody();
    }

    @SuppressWarnings("unchecked")
    private String issue(String customer, LocalDate notBefore, LocalDate notAfter) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("tenantCode", "T-LIC");
        body.put("customer", customer);
        body.put("edition", "ENTERPRISE");
        body.put("mode", "OFFLINE");
        body.put("modules", List.of("CORE"));
        body.put("features", Map.of());
        body.put("appVersionRange", "1.x");
        body.put("notBefore", notBefore.toString());
        body.put("notAfter", notAfter.toString());
        body.put("concurrency", 1);
        Map<String, Object> d = post("/api/licenses/issue", body, Map.class);
        return (String) d.get("licenseId");
    }

    @Test
    @SuppressWarnings("unchecked")
    void revokeProducesVerifiableSignedCrl() {
        String id = issue("吊销验证客户", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));
        post("/api/licenses/" + id + "/revoke", Map.of("reason", "test"), Map.class);

        // 公开 CRL 无需鉴权
        ResponseEntity<Map> pub = rest.getForEntity("/pub/crl", Map.class);
        assertEquals(HttpStatus.OK, pub.getStatusCode());
        Map<String, Object> crl = pub.getBody();
        assertTrue(((Number) crl.get("count")).intValue() >= 1);
        List<Map<String, Object>> revoked = (List<Map<String, Object>>) crl.get("revoked");
        assertTrue(revoked.stream().anyMatch(m -> id.equals(m.get("licenseId"))), "CRL 应含被吊销 License");

        // 验签:payloadB64 + signature 用服务端公钥校验通过(离线客户端同样流程)
        byte[] payload = Base64.getUrlDecoder().decode((String) crl.get("payloadB64"));
        byte[] signature = Base64.getUrlDecoder().decode((String) crl.get("signature"));
        assertTrue(sig.verify(payload, signature), "签名 CRL 必须可被公钥验签");

        // 公开公钥集可拉取
        assertEquals(HttpStatus.OK, rest.getForEntity("/pub/license/public-keys", List.class).getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void autoExpireMarksOverdue() {
        String id = issue("到期客户", LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        Map<String, Object> r = post("/api/licenses/run-auto-expire", Map.of(), Map.class);
        assertTrue(((Number) r.get("expired")).intValue() >= 1, "应停用至少一张到期 License");

        Map<String, Object> detail = rest.exchange("/api/licenses/" + id, HttpMethod.GET,
                new HttpEntity<>(auth), Map.class).getBody();
        assertEquals("EXPIRED", detail.get("status"));
    }
}
