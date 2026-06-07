package com.codeman.platform.features;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端：须知与授权 / 智能客服(降级) / 合同签约 全链路(全栈启动 H2 + Flyway + RBAC 种子)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeaturesIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    private HttpHeaders auth;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        ResponseEntity<Map> r = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "8888"), Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        auth = new HttpHeaders();
        auth.setBearerAuth((String) r.getBody().get("token"));
        auth.setContentType(MediaType.APPLICATION_JSON);
    }

    private <T> T post(String url, Object body, Class<T> type) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, auth), type).getBody();
    }

    private ResponseEntity<String> get(String url) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(auth), String.class);
    }

    // ---- 须知 + 授权 ----
    @Test
    @SuppressWarnings("unchecked")
    void noticeLifecycleAndConsent() {
        Map<String, Object> created = post("/api/notices",
                Map.of("type", "TERMS", "title", "条款", "contentHtml", "<p>hi</p>", "forceAck", true), Map.class);
        Integer id = (Integer) created.get("id");
        assertEquals("DRAFT", created.get("status"));

        Map<String, Object> pub = post("/api/notices/" + id + "/publish", Map.of(), Map.class);
        assertEquals("PUBLISHED", pub.get("status"));

        // 公开页可取到生效须知
        assertTrue(get("/pub/notices/active?type=TERMS").getBody().contains("条款"));

        // 强制确认须知应出现在 pending，确认后清空
        assertTrue(get("/api/notices/pending").getBody().contains("条款"));
        Map<String, Object> consent = post("/api/notices/" + id + "/ack", Map.of(), Map.class);
        assertEquals("GRANTED", consent.get("action"));
        assertEquals("admin", consent.get("subject"));
        assertFalse(get("/api/notices/pending").getBody().contains("\"id\":" + id), "确认后不应再 pending");

        // 已发布须知不可再编辑
        ResponseEntity<String> edit = rest.exchange("/api/notices/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "x", "contentHtml", "y", "forceAck", false), auth), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, edit.getStatusCode());
    }

    // ---- 智能客服(未配置上游 → 知识库降级) ----
    @Test
    @SuppressWarnings("unchecked")
    void csStatusAndDegradedChat() {
        Map<String, Object> st = get("/api/cs/status").getBody() == null ? null
                : rest.exchange("/api/cs/status", HttpMethod.GET, new HttpEntity<>(auth), Map.class).getBody();
        assertNotNull(st);
        assertEquals(Boolean.FALSE, st.get("ready"), "默认未接入大模型");
        assertTrue(((Number) st.get("kbSize")).intValue() > 0, "知识库已加载");

        ResponseEntity<String> chat = rest.exchange("/api/cs/chat", HttpMethod.POST,
                new HttpEntity<>(Map.of("question", "License 是怎么签发的?"), auth), String.class);
        assertEquals(HttpStatus.OK, chat.getStatusCode());
        String body = chat.getBody();
        assertNotNull(body);
        assertTrue(body.contains("event:meta"), "应有 SSE meta 事件");
        assertTrue(body.contains("License"), "降级回复应含知识库命中内容");
    }

    // ---- 合同签约：创建 → 发起 → 多方签署 → 自动出账 ----
    @Test
    @SuppressWarnings("unchecked")
    void contractSignFlowAndAutoBilling() {
        Map<String, Object> c = post("/api/contracts", Map.of(
                "customer", "测试客户", "planCode", "ENTERPRISE", "amount", 12000,
                "contentHtml", "<p>协议</p>",
                "parties", List.of(Map.of("name", "甲方公司", "partyRole", "甲方"),
                        Map.of("name", "乙方公司", "partyRole", "乙方"))), Map.class);
        Integer cid = (Integer) c.get("id");
        assertEquals("DRAFT", c.get("status"));

        Map<String, Object> sent = post("/api/contracts/" + cid + "/send", Map.of(), Map.class);
        assertEquals("SENT", sent.get("status"));
        assertNotNull(sent.get("contentHash"), "发起签署应生成内容存证哈希");
        assertNotNull(sent.get("contractNo"));

        // 取签署方 id
        Map<String, Object> detail = rest.exchange("/api/contracts/" + cid, HttpMethod.GET,
                new HttpEntity<>(auth), Map.class).getBody();
        List<Map<String, Object>> parties = (List<Map<String, Object>>) detail.get("parties");
        assertEquals(2, parties.size());

        Map<String, Object> afterFirst = post("/api/contracts/" + cid + "/sign/" + parties.get(0).get("id"), Map.of(), Map.class);
        assertEquals("SIGNING", afterFirst.get("status"), "部分签署应为 SIGNING");

        Map<String, Object> afterSecond = post("/api/contracts/" + cid + "/sign/" + parties.get(1).get("id"), Map.of(), Map.class);
        assertEquals("SIGNED", afterSecond.get("status"), "全部签署应为 SIGNED");
        assertNotNull(afterSecond.get("signedAt"));

        // 金额>0 → 自动生成 CONTRACT 账单
        assertTrue(get("/api/invoices").getBody().contains("\"type\":\"CONTRACT\""), "应自动出账");

        // 已签署不可作废
        ResponseEntity<String> v = rest.exchange("/api/contracts/" + cid + "/void", HttpMethod.POST,
                new HttpEntity<>(auth), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, v.getStatusCode());
    }
}
