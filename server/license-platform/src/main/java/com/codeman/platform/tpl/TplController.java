package com.codeman.platform.tpl;

import com.codeman.platform.rbac.service.RequirePerm;
import com.codeman.platform.tpl.TplDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 模板资产管理(运营端):CRUD / 审批流 / 版本与回滚 / 导出 / 模板库密钥。 */
@RestController
@RequestMapping("/api/tpl")
public class TplController {

    private final TplService service;

    public TplController(TplService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("tpl:view")
    public List<TemplateView> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String keyword) {
        return service.list(status, keyword);
    }

    @GetMapping("/keys")
    @RequirePerm("tpl:view")
    public List<GalleryKeyView> keys() {
        return service.listKeys();
    }

    @PostMapping("/keys/{tenantCode}")
    @RequirePerm("tpl:edit")
    public GalleryKeyView ensureKey(@PathVariable String tenantCode) {
        return service.ensureKey(tenantCode);
    }

    @PostMapping("/keys/{tenantCode}/reset")
    @RequirePerm("tpl:edit")
    public GalleryKeyView resetKey(@PathVariable String tenantCode) {
        return service.resetKey(tenantCode);
    }

    @GetMapping("/{code}")
    @RequirePerm("tpl:view")
    public TemplateDetail detail(@PathVariable String code) {
        return service.detail(code);
    }

    @GetMapping("/{code}/versions")
    @RequirePerm("tpl:view")
    public List<VersionView> versions(@PathVariable String code) {
        return service.versions(code);
    }

    /** 导出模板 JSON(下载;审计记录导出人)。 */
    @GetMapping("/{code}/export")
    @RequirePerm("tpl:export")
    public ResponseEntity<byte[]> export(@PathVariable String code) {
        byte[] body = service.exportContent(code).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + code + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping
    @RequirePerm("tpl:edit")
    public TemplateView create(@Valid @RequestBody CreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{code}")
    @RequirePerm("tpl:edit")
    public TemplateView update(@PathVariable String code, @RequestBody UpdateRequest req) {
        return service.update(code, req);
    }

    @PostMapping("/{code}/submit")
    @RequirePerm("tpl:edit")
    public VersionView submit(@PathVariable String code, @RequestBody(required = false) NoteRequest req) {
        return service.submit(code, req == null ? null : req.note());
    }

    @PostMapping("/{code}/approve")
    @RequirePerm("tpl:approve")
    public TemplateView approve(@PathVariable String code, @RequestBody(required = false) NoteRequest req) {
        return service.approve(code, req == null ? null : req.note());
    }

    @PostMapping("/{code}/reject")
    @RequirePerm("tpl:approve")
    public TemplateView reject(@PathVariable String code, @RequestBody(required = false) NoteRequest req) {
        return service.reject(code, req == null ? null : req.note());
    }

    @PostMapping("/{code}/rollback/{version}")
    @RequirePerm("tpl:edit")
    public TemplateView rollback(@PathVariable String code, @PathVariable int version) {
        return service.rollback(code, version);
    }

    @PostMapping("/{code}/disable")
    @RequirePerm("tpl:edit")
    public TemplateView disable(@PathVariable String code) {
        return service.setDisabled(code, true);
    }

    @PostMapping("/{code}/enable")
    @RequirePerm("tpl:edit")
    public TemplateView enable(@PathVariable String code) {
        return service.setDisabled(code, false);
    }

    @DeleteMapping("/{code}")
    @RequirePerm("tpl:delete")
    public void delete(@PathVariable String code) {
        service.delete(code);
    }
}
