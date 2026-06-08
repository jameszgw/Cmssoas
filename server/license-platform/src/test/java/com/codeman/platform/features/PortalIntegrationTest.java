package com.codeman.platform.features;

import com.codeman.platform.tenant.domain.Tenant;
import com.codeman.platform.tenant.domain.TenantStatus;
import com.codeman.platform.tenant.repo.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 端到端:租户自助门户——运营开通 → 客户登录 → 只读全景;含越权/错码防护。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PortalIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired TenantRepository tenantRepo;
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

    private Tenant seedTenant(String code) {
        Tenant t = new Tenant();
        t.setCode(code);
        t.setName("门户测试租户");
        t.setPlanCode("ENTERPRISE");
        t.setPlanKey("ent");
        t.setVersion("v1");
        t.setIsolation("SCHEMA");
        t.setMode("ONLINE");
        t.setStatus(TenantStatus.ACTIVE);
        t.setAdminEmail("a@b.com");
        t.setEmailSent(true);
        t.setExpireAt(LocalDate.now().plusYears(1));
        t.setCreatedAt(LocalDateTime.now());
        return tenantRepo.save(t);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enableLoginAndScopedOverview() {
        String code = "T-PORTAL1";
        seedTenant(code);

        // 运营开通 → 拿访问码
        Map<String, Object> portal = rest.exchange("/api/portal-admin/" + code + "/enable", HttpMethod.POST,
                new HttpEntity<>(auth), Map.class).getBody();
        String accessCode = (String) portal.get("accessCode");
        assertNotNull(accessCode);

        // 错误访问码 → 401
        ResponseEntity<String> bad = rest.postForEntity("/pub/portal/login",
                Map.of("tenantCode", code, "accessCode", "WRONG"), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, bad.getStatusCode());

        // 正确登录 → 拿门户 token
        Map<String, Object> login = rest.postForEntity("/pub/portal/login",
                Map.of("tenantCode", code, "accessCode", accessCode), Map.class).getBody();
        String token = (String) login.get("token");
        assertNotNull(token);
        assertEquals("门户测试租户", login.get("tenantName"));

        // 无 token 访问 overview → 401(/pub 被过滤器放行,但门户自校验拦截)
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.getForEntity("/pub/portal/overview", String.class).getStatusCode());

        // 带门户 token → 200,且仅本租户数据
        HttpHeaders ph = new HttpHeaders();
        ph.setBearerAuth(token);
        Map<String, Object> ov = rest.exchange("/pub/portal/overview", HttpMethod.GET,
                new HttpEntity<>(ph), Map.class).getBody();
        assertEquals(code, ov.get("tenantCode"));
        assertNotNull(ov.get("kpi"));
        assertNotNull(ov.get("licenses"));
    }

    @Test
    void enableMissingTenantReturns404() {
        ResponseEntity<String> r = rest.exchange("/api/portal-admin/T-NOPE/enable", HttpMethod.POST,
                new HttpEntity<>(auth), String.class);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }
}
