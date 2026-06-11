package com.codeman.platform.features;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模板资产管理端到端:
 * 创建草稿 → 审批中只读 → 审批通过生效 → 版本留档/回滚 → 导出/审计;
 * 模板库密钥 → CmPrint 契约公开端点(列表/上传送审/use 计数/属主删除)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TplAssetIntegrationTest {

    @Autowired TestRestTemplate rest;
    private HttpHeaders auth;

    private static final String TPL_V1 = "{\"panels\":[{\"index\":0,\"printElements\":[]}]}";
    private static final String TPL_V2 = "{\"panels\":[{\"index\":0,\"printElements\":[{\"o\":1}]}]}";

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
    private String createTemplate(String name) {
        ResponseEntity<Map> r = post("/api/tpl", Map.of(
                "name", name, "content", TPL_V1, "tags", "出库单"), Map.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("DRAFT", r.getBody().get("status"));
        return (String) r.getBody().get("code");
    }

    @Test
    @SuppressWarnings("unchecked")
    void approvalFlowFreezesEditingAndActivatesVersion() {
        String code = createTemplate("审批流模板");

        // 送审 → PENDING(只读)
        Map<String, Object> v1 = post("/api/tpl/" + code + "/submit", Map.of("note", "首版"), Map.class).getBody();
        assertEquals(1, v1.get("version"));
        assertEquals(HttpStatus.BAD_REQUEST,
                rest.exchange("/api/tpl/" + code, HttpMethod.PUT,
                        new HttpEntity<>(Map.of("content", TPL_V2), auth), Map.class).getStatusCode(),
                "审批中应只读");

        // 审批通过 → 生效
        Map<String, Object> t = post("/api/tpl/" + code + "/approve", Map.of("note", "通过"), Map.class).getBody();
        assertEquals("APPROVED", t.get("status"));
        assertEquals(1, t.get("currentVersion"));

        // 改草稿 → 再送审 → 驳回 → 仍是 v1 生效且可继续编辑
        rest.exchange("/api/tpl/" + code, HttpMethod.PUT,
                new HttpEntity<>(Map.of("content", TPL_V2), auth), Map.class);
        post("/api/tpl/" + code + "/submit", Map.of(), Map.class);
        Map<String, Object> rejected = post("/api/tpl/" + code + "/reject", Map.of("note", "退回"), Map.class).getBody();
        assertEquals("APPROVED", rejected.get("status"));
        assertEquals(1, rejected.get("currentVersion"));

        // 版本列表:v2 REJECTED、v1 APPROVED,含提交/审批人与意见
        List<Map<String, Object>> versions = rest.exchange("/api/tpl/" + code + "/versions",
                HttpMethod.GET, new HttpEntity<>(auth),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
        assertEquals(2, versions.size());
        assertEquals("REJECTED", versions.get(0).get("status"));
        assertEquals("退回", versions.get(0).get("reviewNote"));
        assertEquals("APPROVED", versions.get(1).get("status"));
        assertEquals("admin", versions.get(1).get("submittedBy"));

        // 回滚:草稿恢复 v1 内容(detail.draftContent)
        post("/api/tpl/" + code + "/rollback/1", null, Map.class);
        Map<String, Object> detail = get("/api/tpl/" + code, Map.class).getBody();
        assertEquals(TPL_V1, detail.get("draftContent"));

        // 导出 = 生效内容
        ResponseEntity<String> exp = get("/api/tpl/" + code + "/export", String.class);
        assertEquals(TPL_V1, exp.getBody());

        // 审计轨迹可查(CmPrint 审计查询纳入 TPL_*)
        Map<String, Object> audit = get("/api/cmprint/audit?keyword=" + code, Map.class).getBody();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) audit.get("rows");
        List<Object> actions = rows.stream().map(a -> a.get("action")).toList();
        assertTrue(actions.containsAll(List.of("TPL_CREATE", "TPL_SUBMIT", "TPL_APPROVE", "TPL_REJECT", "TPL_EXPORT")),
                "应含完整审批轨迹,实际:" + actions);
    }

    @Test
    @SuppressWarnings("unchecked")
    void galleryServesCmprintContract() {
        // 生效一个平台公共模板
        String code = createTemplate("公共回单");
        post("/api/tpl/" + code + "/submit", Map.of(), Map.class);
        post("/api/tpl/" + code + "/approve", Map.of(), Map.class);

        // 开通公共库密钥
        Map<String, Object> key = post("/api/tpl/keys/PUBLIC", null, Map.class).getBody();
        String k = (String) key.get("galleryKey");
        String base = "/pub/cmprint/gallery/" + k + "/api/templates";

        // 契约:GET 列表 {success,data:[{id,name,template,author,useCount}]}(匿名,无 JWT)
        ResponseEntity<Map> list = rest.getForEntity(base, Map.class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertEquals(Boolean.TRUE, list.getBody().get("success"));
        List<Map<String, Object>> data = (List<Map<String, Object>>) list.getBody().get("data");
        Map<String, Object> node = data.stream().filter(n -> code.equals(n.get("id"))).findFirst().orElseThrow();
        assertEquals("公共回单", node.get("name"));
        assertTrue(((Map<String, Object>) node.get("template")).containsKey("panels"));

        // use 计数 +1
        Map<String, Object> used = rest.postForEntity(base + "/" + code + "/use", null, Map.class)
                .getBody();
        assertEquals(1, ((Map<String, Object>) used.get("data")).get("useCount"));

        // 设计器上传 → PENDING,不出现在列表;审批通过后出现
        Map<String, Object> saved = rest.postForEntity(base, Map.of(
                "name", "租户上传单", "owner", "designerA",
                "template", Map.of("panels", List.of(Map.of("index", 0)))), Map.class).getBody();
        assertEquals(Boolean.TRUE, saved.get("success"));
        String upCode = (String) ((Map<String, Object>) saved.get("data")).get("id");
        List<Map<String, Object>> after = (List<Map<String, Object>>) rest.getForEntity(base, Map.class)
                .getBody().get("data");
        assertTrue(after.stream().noneMatch(n -> upCode.equals(n.get("id"))), "未审批不进模板库");

        post("/api/tpl/" + upCode + "/approve", Map.of("note", "上架"), Map.class);
        List<Map<String, Object>> approved = (List<Map<String, Object>>) rest.getForEntity(base, Map.class)
                .getBody().get("data");
        assertTrue(approved.stream().anyMatch(n -> upCode.equals(n.get("id"))));

        // 属主删除:已生效 → 拒绝(走运营端);非属主 → 拒绝
        Map<String, Object> rm = rest.exchange(base + "/" + upCode + "?owner=designerA",
                HttpMethod.DELETE, HttpEntity.EMPTY, Map.class).getBody();
        assertEquals(Boolean.FALSE, rm.get("success"));

        // 无效密钥 → success:false
        Map<String, Object> bad = rest.getForEntity("/pub/cmprint/gallery/not-a-key/api/templates", Map.class)
                .getBody();
        assertEquals(Boolean.FALSE, bad.get("success"));
    }

    @Test
    void galleryKeyResetInvalidatesOldUrl() {
        post("/api/tpl/keys/T-TPL-01", null, Map.class);
        String k1 = (String) post("/api/tpl/keys/T-TPL-01", null, Map.class).getBody().get("galleryKey");
        String k2 = (String) post("/api/tpl/keys/T-TPL-01/reset", null, Map.class).getBody().get("galleryKey");
        assertNotEquals(k1, k2);
        assertEquals(Boolean.FALSE,
                rest.getForEntity("/pub/cmprint/gallery/" + k1 + "/api/templates", Map.class)
                        .getBody().get("success"));
        assertEquals(Boolean.TRUE,
                rest.getForEntity("/pub/cmprint/gallery/" + k2 + "/api/templates", Map.class)
                        .getBody().get("success"));
    }

    @Test
    void invalidContentRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/tpl", Map.of(
                "name", "坏模板", "content", "{\"notPanels\":1}"), Map.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/tpl", Map.of(
                "name", "坏JSON", "content", "{{{"), Map.class).getStatusCode());
    }
}
