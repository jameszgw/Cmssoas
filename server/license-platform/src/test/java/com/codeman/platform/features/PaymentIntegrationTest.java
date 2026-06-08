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

/** 端到端:支付/收款闭环(沙箱)——发起收款 → 模拟支付 → 账单自动收款;含幂等与公开回调。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIntegrationTest {

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

    /** 借合同签署自动出账,得到一张 PENDING 账单 id。 */
    @SuppressWarnings("unchecked")
    private int pendingInvoiceViaContract() {
        Map<String, Object> c = post("/api/contracts", Map.of(
                "customer", "支付测试客户", "amount", 9900, "contentHtml", "<p>x</p>",
                "parties", List.of(Map.of("name", "甲", "partyRole", "甲方"))), Map.class);
        int cid = (Integer) c.get("id");
        post("/api/contracts/" + cid + "/send", Map.of(), Map.class);
        Map<String, Object> d = rest.exchange("/api/contracts/" + cid, HttpMethod.GET, new HttpEntity<>(auth), Map.class).getBody();
        int pid = (Integer) ((List<Map<String, Object>>) d.get("parties")).get(0).get("id");
        post("/api/contracts/" + cid + "/sign/" + pid, Map.of(), Map.class);   // 单方签署即 SIGNED → 出账
        List<Map<String, Object>> invs = rest.exchange("/api/invoices", HttpMethod.GET,
                new HttpEntity<>(auth), List.class).getBody();
        return invs.stream().filter(i -> "PENDING".equals(i.get("status")))
                .map(i -> (Integer) i.get("id")).findFirst().orElseThrow();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sandboxPaymentClosesInvoice() {
        int invId = pendingInvoiceViaContract();

        Map<String, Object> pay = post("/api/payments", Map.of("invoiceId", invId), Map.class);
        assertEquals("CREATED", pay.get("status"));
        assertEquals("MOCK", pay.get("channel"));
        assertNotNull(pay.get("qrContent"), "应返回二维码内容");
        Integer payId = (Integer) pay.get("id");

        Map<String, Object> confirmed = post("/api/payments/" + payId + "/sandbox-confirm", Map.of(), Map.class);
        assertEquals("PAID", confirmed.get("status"));

        // 账单已联动收款
        List<Map<String, Object>> invs = rest.exchange("/api/invoices", HttpMethod.GET,
                new HttpEntity<>(auth), List.class).getBody();
        String st = invs.stream().filter(i -> invId == (Integer) i.get("id"))
                .map(i -> (String) i.get("status")).findFirst().orElse("");
        assertEquals("PAID", st, "支付成功后账单应为已付款");

        // 幂等:再次确认不报错,仍为 PAID
        Map<String, Object> again = post("/api/payments/" + payId + "/sandbox-confirm", Map.of(), Map.class);
        assertEquals("PAID", again.get("status"));
    }

    @Test
    void notifyEndpointIsPublic() {
        // 公开回调不需要鉴权(未知单号返回 FAIL,但不应 401/403)
        ResponseEntity<String> r = rest.postForEntity("/pub/payments/notify/MOCK", "PAY-NOPE", String.class);
        assertNotEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertNotEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals("FAIL", r.getBody());
    }
}
