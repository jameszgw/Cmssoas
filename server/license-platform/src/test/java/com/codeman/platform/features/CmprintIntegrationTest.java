package com.codeman.platform.features;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CmPrint 商业授权集成端到端:
 * 档位矩阵 → 按档位+能力微调签发(claims 与 CmPrint resolveEdition 契约一致)→ 验签 →
 * 本产品列表过滤 → 审计查询(动作/关键字过滤) → CMPRINT 套餐订阅自动签发。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CmprintIntegrationTest {

    @Autowired TestRestTemplate rest;
    private HttpHeaders auth;

    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> issueCmprint(String edition, Map<String, Boolean> overrides, String customer) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("tenantCode", "T-CMP-001");
        body.put("customer", customer);
        body.put("edition", edition);
        if (overrides != null) body.put("overrides", overrides);
        body.put("notBefore", TODAY.toString());
        body.put("notAfter", TODAY.plusYears(1).toString());
        ResponseEntity<Map> r = post("/api/cmprint/licenses/issue", body, Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return r.getBody();
    }

    @Test
    @SuppressWarnings("unchecked")
    void editionsExposeCapabilityMatrix() {
        ResponseEntity<Map> r = get("/api/cmprint/editions", Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        Map<String, Object> v = r.getBody();
        assertEquals("CMPRINT", v.get("productCode"));
        List<String> keys = (List<String>) v.get("capabilityKeys");
        assertEquals(37, keys.size(), "能力键应与 CmPrint CAPABILITY_KEYS 同数");
        assertTrue(keys.contains("directPrint") && keys.contains("watermark"));

        List<Map<String, Object>> editions = (List<Map<String, Object>>) v.get("editions");
        assertEquals(List.of("COMMUNITY", "PROFESSIONAL", "ENTERPRISE"),
                editions.stream().map(e -> e.get("edition")).toList());
        Map<String, Object> community = editions.get(0);
        Map<String, Object> preset = (Map<String, Object>) community.get("preset");
        assertEquals(Boolean.FALSE, preset.get("exportPdf"));
        Map<String, Object> caps = (Map<String, Object>) community.get("capabilities");
        assertEquals(37, caps.size(), "解析结果应是全键布尔表");
        assertEquals(Boolean.TRUE, caps.get("preview"));
        assertEquals(Boolean.FALSE, caps.get("watermark"));
        // 企业版全开
        Map<String, Object> ent = (Map<String, Object>) editions.get(2).get("capabilities");
        assertTrue(ent.values().stream().allMatch(Boolean.TRUE::equals));
    }

    @Test
    @SuppressWarnings("unchecked")
    void issueProfessionalWithOverrideSignsResolvableClaims() throws Exception {
        // 小写档位应被归一;专业版预设关 directPrint,合同微调单独放开
        Map<String, Object> d = issueCmprint("professional", Map.of("directPrint", true), "华印科技");
        String id = (String) d.get("licenseId");
        assertEquals("CMPRINT", d.get("productCode"));
        assertEquals("PROFESSIONAL", d.get("edition"));

        // 下载 .lic 并走平台验签(模拟 SDK):claims 即 resolveEdition 的输入
        ResponseEntity<byte[]> lic = get("/api/licenses/" + id + "/download", byte[].class);
        assertEquals(HttpStatus.OK, lic.getStatusCode());
        ResponseEntity<Map> ver = post("/api/licenses/verify",
                Map.of("lic", new String(lic.getBody())), Map.class);
        assertEquals(Boolean.TRUE, ver.getBody().get("valid"));
        Map<String, Object> claims = (Map<String, Object>) ver.getBody().get("claims");
        assertEquals("CMPRINT", claims.get("productCode"));
        assertEquals("PROFESSIONAL", claims.get("edition"));
        Map<String, Object> features = (Map<String, Object>) claims.get("features");
        assertEquals(Boolean.TRUE, features.get("directPrint"), "微调应固化进 features");
        assertEquals(Boolean.FALSE, features.get("templateGallery"), "档位预设应固化进 features");
        assertEquals(List.of("DESIGN", "DATA", "OUTPUT", "EXPORT", "TEMPLATE"), claims.get("modules"));
        assertEquals(">=0.5.0 <1.0.0", claims.get("appVersionRange"));
    }

    @Test
    void unknownEditionOrCapabilityRejected() {
        Map<String, Object> bad = new java.util.LinkedHashMap<>();
        bad.put("tenantCode", "T-CMP-001");
        bad.put("customer", "无效请求");
        bad.put("edition", "ULTIMATE");
        bad.put("notBefore", TODAY.toString());
        bad.put("notAfter", TODAY.plusYears(1).toString());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/cmprint/licenses/issue", bad, Map.class).getStatusCode());

        bad.put("edition", "ENTERPRISE");
        bad.put("overrides", Map.of("notACapability", true));
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/cmprint/licenses/issue", bad, Map.class).getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cmprintListExcludesOtherProducts() {
        issueCmprint("COMMUNITY", null, "社区客户");
        // 一张非 CmPrint 的通用 License
        post("/api/licenses/issue", Map.of(
                "tenantCode", "T-OTHER", "customer", "其它产品客户", "edition", "BASIC",
                "modules", List.of("RISK"), "features", Map.of(),
                "notBefore", TODAY.toString(), "notAfter", TODAY.plusYears(1).toString()), Map.class);

        ResponseEntity<List<Map<String, Object>>> r = rest.exchange("/api/cmprint/licenses",
                HttpMethod.GET, new HttpEntity<>(auth), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertFalse(r.getBody().isEmpty());
        assertTrue(r.getBody().stream().allMatch(v -> "CMPRINT".equals(v.get("productCode"))),
                "CmPrint 列表只含本产品 License");
    }

    @Test
    @SuppressWarnings("unchecked")
    void auditQueryTracksLifecycleWithFilters() {
        Map<String, Object> d = issueCmprint("ENTERPRISE", null, "审计客户");
        String id = (String) d.get("licenseId");
        post("/api/licenses/" + id + "/revoke", Map.of("reason", "审计测试吊销"), Map.class);

        // 动作过滤:签发动作可查
        Map<String, Object> p1 = get("/api/cmprint/audit?action=CMPRINT_LICENSE_ISSUE", Map.class).getBody();
        List<Map<String, Object>> rows1 = (List<Map<String, Object>>) p1.get("rows");
        assertTrue(rows1.stream().anyMatch(a -> String.valueOf(a.get("detail")).contains(id)));

        // 关键字过滤:按 License 编号检索,通用吊销事件也应纳入 CmPrint 审计范围
        Map<String, Object> p2 = get("/api/cmprint/audit?keyword=" + id, Map.class).getBody();
        List<Map<String, Object>> rows2 = (List<Map<String, Object>>) p2.get("rows");
        assertTrue(rows2.stream().anyMatch(a -> "CMPRINT_LICENSE_ISSUE".equals(a.get("action"))));
        assertTrue(rows2.stream().anyMatch(a -> "LICENSE_REVOKE".equals(a.get("action"))),
                "通用 License 事件按编号纳入 CmPrint 审计");

        // 时间窗:明天起查不到今天的记录
        Map<String, Object> p3 = get("/api/cmprint/audit?keyword=" + id + "&from=" + TODAY.plusDays(1), Map.class).getBody();
        assertEquals(0, ((List<?>) p3.get("rows")).size());

        // CSV 导出
        ResponseEntity<byte[]> csv = get("/api/cmprint/audit/export.csv?keyword=" + id, byte[].class);
        assertEquals(HttpStatus.OK, csv.getStatusCode());
        assertTrue(new String(csv.getBody()).contains(id));
    }

    @Test
    @SuppressWarnings("unchecked")
    void cmprintPlanSubscriptionAutoIssuesCmprintLicense() {
        ResponseEntity<Map> r = post("/api/subscriptions", Map.of(
                "tenantCode", "T-CMP-SUB", "customer", "订阅客户", "planCode", "CMPRINT_PRO",
                "qty", 1, "startAt", TODAY.toString(), "endAt", TODAY.plusYears(1).toString()), Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        String licenseId = (String) r.getBody().get("licenseId");
        assertNotNull(licenseId);

        Map<String, Object> detail = get("/api/licenses/" + licenseId, Map.class).getBody();
        assertEquals("CMPRINT", detail.get("productCode"), "套餐应携带产品编码");
        assertEquals("PROFESSIONAL", detail.get("edition"), "套餐应携带版本档位而非套餐码");
    }
}
