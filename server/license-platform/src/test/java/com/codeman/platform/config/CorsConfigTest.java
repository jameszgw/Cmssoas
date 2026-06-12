package com.codeman.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 浏览器跨域通道回归:/sdk/**(在线激活/心跳)与 /pub/**(模板库/CRL)必须对任意来源放开
 * CORS(无凭据)——CmPrint 等浏览器客户端从集成方域名直调;/api/** 仍按白名单。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsConfigTest {

    @Autowired TestRestTemplate rest;

    private ResponseEntity<Void> preflight(String path, HttpMethod method) {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        HttpHeaders h = new HttpHeaders();
        h.setOrigin("https://customer-app.example.com");
        h.setAccessControlRequestMethod(method);
        return rest.exchange(path, HttpMethod.OPTIONS, new HttpEntity<>(h), Void.class);
    }

    @Test
    void sdkChannelAllowsAnyOrigin() {
        ResponseEntity<Void> r = preflight("/sdk/heartbeat", HttpMethod.POST);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("*", r.getHeaders().getAccessControlAllowOrigin(), "/sdk/** 应对任意来源放开");
    }

    @Test
    void pubGalleryAllowsAnyOrigin() {
        ResponseEntity<Void> r = preflight("/pub/cmprint/gallery/any/api/templates", HttpMethod.GET);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("*", r.getHeaders().getAccessControlAllowOrigin(), "/pub/** 应对任意来源放开");
    }

    @Test
    void apiChannelStaysRestricted() {
        ResponseEntity<Void> r = preflight("/api/licenses", HttpMethod.GET);
        // 未配置白名单来源 → 预检被拒(403),不得回 ACAO:*
        assertNotEquals("*", r.getHeaders().getAccessControlAllowOrigin(), "/api/** 不得对任意来源放开");
    }
}
