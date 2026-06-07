package com.codeman.platform.harden.web;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.harden.domain.HardenConfig;
import com.codeman.platform.harden.domain.HardenJob;
import com.codeman.platform.harden.service.HardenService;
import com.codeman.platform.harden.service.HardenTechnique;
import com.codeman.platform.rbac.service.RequirePerm;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 在线代码加固:租户级模式配置 + 上传 jar 异步加固 + 产物下载。与构建/打包加固并存。 */
@RestController
@RequestMapping("/api/harden")
public class HardenController {

    private final HardenService service;

    public HardenController(HardenService service) {
        this.service = service;
    }

    // ---- 配置 ----
    @GetMapping("/config")
    @RequirePerm("harden:view")
    public HardenConfig config(@RequestParam(defaultValue = "") String tenantCode) {
        return service.config(tenantCode);
    }

    @GetMapping("/configs")
    @RequirePerm("harden:view")
    public List<HardenConfig> configs() {
        return service.allConfigs();
    }

    @PutMapping("/config")
    @RequirePerm("harden:config")
    public HardenConfig saveConfig(@RequestBody Map<String, Object> b) {
        return service.saveConfig(
                String.valueOf(b.getOrDefault("tenantCode", "")),
                String.valueOf(b.getOrDefault("mode", "BUILD")),
                Boolean.parseBoolean(String.valueOf(b.getOrDefault("obfuscate", true))),
                Boolean.parseBoolean(String.valueOf(b.getOrDefault("encryptBind", false))),
                Boolean.parseBoolean(String.valueOf(b.getOrDefault("fatjarEncrypt", false))));
    }

    // ---- 任务 ----
    @GetMapping("/jobs")
    @RequirePerm("harden:view")
    public List<HardenJob> jobs() {
        return service.jobs();
    }

    @GetMapping("/jobs/{id}")
    @RequirePerm("harden:view")
    public HardenJob job(@PathVariable Long id) {
        return service.get(id);
    }

    /** 上传 jar 提交加固任务(multipart)。 */
    @PostMapping("/jobs")
    @RequirePerm("harden:run")
    public HardenJob submit(@RequestParam("file") MultipartFile file,
                            @RequestParam(required = false) String tenantCode,
                            @RequestParam(required = false) String techniques,
                            @RequestParam(required = false) String bindLicense,
                            @RequestParam(required = false) String passphrase,
                            @RequestParam(required = false) String encryptPrefix) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("请上传 jar 文件");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".jar")) throw ApiException.badRequest("仅支持 .jar 文件");
        try (var in = file.getInputStream()) {
            return service.submit(tenantCode, parse(techniques), bindLicense, passphrase, encryptPrefix, name, in);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("提交失败:" + e.getMessage());
        }
    }

    @GetMapping("/jobs/{id}/download")
    @RequirePerm("harden:view")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) throws Exception {
        HardenJob job = service.get(id);
        byte[] data = Files.readAllBytes(service.artifact(id));
        String fn = "hardened-" + job.getSourceName();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        h.setContentDisposition(ContentDisposition.attachment().filename(fn).build());
        return ResponseEntity.ok().headers(h).contentLength(data.length).body(new ByteArrayResource(data));
    }

    private List<HardenTechnique> parse(String csv) {
        List<HardenTechnique> l = new ArrayList<>();
        if (csv == null || csv.isBlank()) return l;
        for (String s : csv.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try { l.add(HardenTechnique.valueOf(s)); }
            catch (IllegalArgumentException ex) { throw ApiException.badRequest("未知加固技术:" + s); }
        }
        return l;
    }
}
