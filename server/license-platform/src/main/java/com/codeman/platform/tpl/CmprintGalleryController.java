package com.codeman.platform.tpl;

import com.codeman.platform.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CmPrint 云端模板库公开端点 —— 严格对齐设计器 cloud-api.js 契约:
 * 设计器把 cloudBaseUrl 配成 `{平台地址}/pub/cmprint/gallery/{galleryKey}`,
 * 其后缀固定为 `/api/templates...`,响应统一 `{success, message?, data}`。
 * 鉴权 = 路径中的租户模板库密钥(运营端「模板资产」页生成/重置);只读列表仅含已审批模板,
 * 设计器上传落为 PENDING 须运营审批后方可见。
 */
@RestController
@RequestMapping("/pub/cmprint/gallery/{galleryKey}/api/templates")
public class CmprintGalleryController {

    private final TplService service;

    public CmprintGalleryController(TplService service) {
        this.service = service;
    }

    private static Map<String, Object> ok(Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("data", data);
        return m;
    }

    private static Map<String, Object> fail(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        return m;
    }

    @GetMapping
    public Map<String, Object> list(@PathVariable String galleryKey) {
        try {
            return ok(service.galleryList(galleryKey));
        } catch (ApiException e) {
            return fail(e.getMessage());
        }
    }

    @PostMapping
    public Map<String, Object> save(@PathVariable String galleryKey, @RequestBody JsonNode body) {
        try {
            return ok(service.gallerySave(galleryKey, body));
        } catch (ApiException e) {
            return fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/use")
    public Map<String, Object> use(@PathVariable String galleryKey, @PathVariable String id) {
        try {
            return ok(service.galleryUse(galleryKey, id));
        } catch (ApiException e) {
            return fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> remove(@PathVariable String galleryKey, @PathVariable String id,
                                      @RequestParam(required = false) String owner) {
        try {
            service.galleryRemove(galleryKey, id, owner);
            return ok(Boolean.TRUE);
        } catch (ApiException e) {
            return fail(e.getMessage());
        }
    }
}
