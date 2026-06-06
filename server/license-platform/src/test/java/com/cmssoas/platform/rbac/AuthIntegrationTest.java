package com.cmssoas.platform.rbac;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 端到端：登录 + JWT 鉴权 + 接口级权限拦截（全栈启动 H2 + Flyway + RBAC 种子）。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @BeforeEach
    void useModernClient() {
        // 用 java.net.http 客户端，避免旧版 HttpURLConnection 在 POST 收到 401 时抛 HttpRetryException
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @SuppressWarnings("unchecked")
    private String login(String user, String pwd) {
        ResponseEntity<Map> r = rest.postForEntity("/api/auth/login",
                Map.of("username", user, "password", pwd), Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return (String) r.getBody().get("token");
    }

    @Test
    void adminLoginAndAccess() {
        String token = login("admin", "8888");
        assertNotNull(token);

        // 无 token → 401
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.getForEntity("/api/licenses", String.class).getStatusCode());

        // 带 token → 200
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        ResponseEntity<String> ok = rest.exchange("/api/licenses", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertEquals(HttpStatus.OK, ok.getStatusCode());
    }

    @Test
    void wrongPasswordRejected() {
        ResponseEntity<Map> r = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "nope"), Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
    }

    @Test
    void permissionEnforcedForNonSuper() {
        String admin = login("admin", "8888");
        HttpHeaders ah = new HttpHeaders();
        ah.setBearerAuth(admin);
        ah.setContentType(MediaType.APPLICATION_JSON);

        // 找 VIEWER 角色 id
        ResponseEntity<java.util.List> roles = rest.exchange("/api/rbac/roles", HttpMethod.GET,
                new HttpEntity<>(ah), java.util.List.class);
        Integer viewerId = null;
        for (Object o : roles.getBody()) {
            Map<?, ?> m = (Map<?, ?>) o;
            if ("VIEWER".equals(m.get("code"))) viewerId = (Integer) m.get("id");
        }
        assertNotNull(viewerId);

        // 创建 VIEWER 用户
        rest.exchange("/api/users", HttpMethod.POST,
                new HttpEntity<>(Map.of("username", "ituser", "roleId", viewerId, "password", "view123"), ah), String.class);

        String vtok = login("ituser", "view123");
        HttpHeaders vh = new HttpHeaders();
        vh.setBearerAuth(vtok);
        vh.setContentType(MediaType.APPLICATION_JSON);

        // VIEWER 有 license:view → 200
        assertEquals(HttpStatus.OK,
                rest.exchange("/api/licenses", HttpMethod.GET, new HttpEntity<>(vh), String.class).getStatusCode());

        // VIEWER 无 license:issue → 403
        String body = "{\"tenantCode\":\"T\",\"customer\":\"c\",\"modules\":[\"RISK\"],\"notBefore\":\"2026-06-06\",\"notAfter\":\"2027-06-06\"}";
        assertEquals(HttpStatus.FORBIDDEN,
                rest.exchange("/api/licenses/issue", HttpMethod.POST, new HttpEntity<>(body, vh), String.class).getStatusCode());
    }
}
