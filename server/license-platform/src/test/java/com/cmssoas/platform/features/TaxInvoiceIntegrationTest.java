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

/** 端到端:正规电子发票——已收款账单申请开票 → 沙箱开具 → 账单已开票(发票号取真实号码)。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaxInvoiceIntegrationTest {

    @Autowired TestRestTemplate rest;
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
    private int paidInvoice() {
        Map<String, Object> c = post("/api/contracts", Map.of("customer", "开票客户", "amount", 20000,
                "contentHtml", "<p>x</p>", "parties", List.of(Map.of("name", "甲", "partyRole", "甲方"))), Map.class);
        int cid = (Integer) c.get("id");
        post("/api/contracts/" + cid + "/send", Map.of(), Map.class);
        Map<String, Object> d = rest.exchange("/api/contracts/" + cid, HttpMethod.GET, new HttpEntity<>(auth), Map.class).getBody();
        int pid = (Integer) ((List<Map<String, Object>>) d.get("parties")).get(0).get("id");
        post("/api/contracts/" + cid + "/sign/" + pid, Map.of(), Map.class);
        List<Map<String, Object>> invs = rest.exchange("/api/invoices", HttpMethod.GET, new HttpEntity<>(auth), List.class).getBody();
        int invId = invs.stream().filter(i -> "PENDING".equals(i.get("status"))).map(i -> (Integer) i.get("id")).findFirst().orElseThrow();
        Map<String, Object> pay = post("/api/payments", Map.of("invoiceId", invId), Map.class);
        post("/api/payments/" + pay.get("id") + "/sandbox-confirm", Map.of(), Map.class);
        return invId;
    }

    @Test
    @SuppressWarnings("unchecked")
    void issueEInvoiceMarksInvoiced() {
        int invId = paidInvoice();

        // 专票缺税号 → 400
        ResponseEntity<String> bad = rest.exchange("/api/invoices/" + invId + "/e-invoice", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "某公司", "type", "SPECIAL"), auth), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());

        // 普票开具
        Map<String, Object> ti = post("/api/invoices/" + invId + "/e-invoice",
                Map.of("title", "某某科技有限公司", "type", "NORMAL", "email", "a@b.com"), Map.class);
        assertEquals("ISSUED", ti.get("status"));
        assertNotNull(ti.get("invoiceCode"));
        assertNotNull(ti.get("invoiceSerial"));
        assertNotNull(ti.get("pdfUrl"));

        // 账单 → INVOICED,发票号=真实发票号码
        List<Map<String, Object>> invs = rest.exchange("/api/invoices", HttpMethod.GET, new HttpEntity<>(auth), List.class).getBody();
        Map<String, Object> inv = invs.stream().filter(i -> invId == (Integer) i.get("id")).findFirst().orElseThrow();
        assertEquals("INVOICED", inv.get("status"));
        assertEquals(ti.get("invoiceSerial"), inv.get("invoiceNo"));
    }
}
