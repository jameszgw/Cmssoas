package com.cmssoas.platform.features;

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

/** 端到端:统一客户主数据 + 客户360 聚合(按名称归集合同/账单)。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerIntegrationTest {

    @Autowired
    TestRestTemplate rest;
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

    @Test
    @SuppressWarnings("unchecked")
    void customer360AggregatesByName() {
        String name = "聚合测试客户";
        Map<String, Object> cust = post("/api/customers", Map.of("name", name, "contact", "张三"), Map.class);
        Integer cid = (Integer) cust.get("id");
        assertNotNull(cust.get("code"));

        // 重名拒绝
        ResponseEntity<String> dup = rest.exchange("/api/customers", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name), auth), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, dup.getStatusCode());

        // 同名合同 → 签署出账
        Map<String, Object> c = post("/api/contracts", Map.of(
                "customer", name, "amount", 30000, "contentHtml", "<p>x</p>",
                "parties", List.of(Map.of("name", "甲", "partyRole", "甲方"))), Map.class);
        int ctId = (Integer) c.get("id");
        post("/api/contracts/" + ctId + "/send", Map.of(), Map.class);
        Map<String, Object> d = rest.exchange("/api/contracts/" + ctId, HttpMethod.GET, new HttpEntity<>(auth), Map.class).getBody();
        int pid = (Integer) ((List<Map<String, Object>>) d.get("parties")).get(0).get("id");
        post("/api/contracts/" + ctId + "/sign/" + pid, Map.of(), Map.class);

        // 客户360 聚合
        Map<String, Object> ov = rest.exchange("/api/customers/" + cid + "/overview", HttpMethod.GET,
                new HttpEntity<>(auth), Map.class).getBody();
        Map<String, Object> kpi = (Map<String, Object>) ov.get("kpi");
        assertEquals(1, ((Number) kpi.get("contractCount")).intValue(), "应聚合到该客户合同");
        assertEquals(30000, ((Number) kpi.get("signedAmount")).intValue());
        assertTrue(((Number) kpi.get("invoiceCount")).intValue() >= 1, "签署出账应聚合到账单");
        assertEquals(1, ((List<?>) ov.get("contracts")).size());
    }
}
