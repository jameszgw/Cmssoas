package com.codeman.platform.harden.service;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.config.AppProperties;
import com.codeman.platform.harden.domain.HardenConfig;
import com.codeman.platform.harden.domain.HardenJob;
import com.codeman.platform.harden.repo.HardenConfigRepository;
import com.codeman.platform.harden.repo.HardenJobRepository;
import com.codeman.platform.license.service.LicenseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 在线代码加固编排:租户级模式/默认技术配置(任务可覆盖)、上传源 jar、异步按技术流水线加固、产物下载。
 * 与"构建/打包"加固并存:仅当租户模式为 ONLINE/BOTH 才允许在线加固。
 */
@Service
public class HardenService {

    private static final Logger log = LoggerFactory.getLogger(HardenService.class);
    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String[] RUNTIME_CLASSES = {
            "com.codeman.platform.harden.runtime.HardenLauncher",
            "com.codeman.platform.harden.runtime.HardenClassLoader"
    };

    private final HardenConfigRepository configRepo;
    private final HardenJobRepository jobRepo;
    private final AppProperties props;
    private final LicenseService licenseService;
    private final AuditWriter audit;
    private final Map<HardenTechnique, HardenProvider> providers;
    private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "harden-worker"); t.setDaemon(true); return t;
    });

    public HardenService(HardenConfigRepository configRepo, HardenJobRepository jobRepo, AppProperties props,
                         LicenseService licenseService, AuditWriter audit, List<HardenProvider> providerList) {
        this.configRepo = configRepo;
        this.jobRepo = jobRepo;
        this.props = props;
        this.licenseService = licenseService;
        this.audit = audit;
        this.providers = new EnumMap<>(HardenTechnique.class);
        for (HardenProvider p : providerList) providers.put(p.technique(), p);
    }

    // ---- 配置(租户级) ----
    public HardenConfig config(String tenantCode) {
        return configRepo.findByTenantCode(tenantCode).orElseGet(() -> {
            HardenConfig c = new HardenConfig();
            c.setTenantCode(tenantCode);
            c.setMode("BUILD");
            c.setUpdatedAt(LocalDateTime.now());
            return c;
        });
    }

    public HardenConfig saveConfig(String tenantCode, String mode, boolean obf, boolean encBind, boolean fatjar) {
        HardenConfig c = configRepo.findByTenantCode(tenantCode).orElseGet(HardenConfig::new);
        c.setTenantCode(tenantCode);
        c.setMode(normalizeMode(mode));
        c.setObfuscate(obf);
        c.setEncryptBind(encBind);
        c.setFatjarEncrypt(fatjar);
        c.setUpdatedAt(LocalDateTime.now());
        configRepo.save(c);
        audit.log(null, "HARDEN_CONFIG", "租户 " + tenantCode + " 加固模式=" + c.getMode());
        return c;
    }

    private String normalizeMode(String m) {
        m = m == null ? "BUILD" : m.trim().toUpperCase();
        return Set.of("BUILD", "ONLINE", "BOTH").contains(m) ? m : "BUILD";
    }

    public List<HardenConfig> allConfigs() { return configRepo.findAll(); }

    // ---- 任务 ----
    public List<HardenJob> jobs() { return jobRepo.findAllByOrderByCreatedAtDesc(); }

    public HardenJob get(Long id) {
        return jobRepo.findById(id).orElseThrow(() -> ApiException.notFound("加固任务不存在"));
    }

    /**
     * 提交在线加固任务:落库 + 保存上传源 + 异步执行流水线。
     * @param techniques 顺序技术列表(为空则用租户默认)
     */
    public HardenJob submit(String tenantCode, List<HardenTechnique> techniques, String bindLicense,
                            String passphrase, String encryptPrefix, String sourceName, InputStream sourceIn) {
        HardenConfig cfg = config(tenantCode == null ? "" : tenantCode);
        if (tenantCode != null && !tenantCode.isBlank() && "BUILD".equals(cfg.getMode()))
            throw ApiException.badRequest("该租户加固模式为「构建自建」,未开启在线加固(请在加固设置改为 ONLINE/BOTH)");

        List<HardenTechnique> techs = (techniques == null || techniques.isEmpty()) ? defaultTechs(cfg) : techniques;
        if (techs.isEmpty()) throw ApiException.badRequest("未选择任何加固技术");
        for (HardenTechnique t : techs) {
            HardenProvider p = providers.get(t);
            if (p == null || !p.available()) throw ApiException.badRequest("加固器不可用:" + t);
        }

        String jobNo = "HJ-" + LocalDateTime.now().format(NO);
        Path dir = Paths.get(props.getHarden().getWorkDir(), jobNo);
        Path src = dir.resolve("source.jar");
        long size;
        try {
            Files.createDirectories(dir);
            size = Files.copy(sourceIn, src, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw ApiException.badRequest("保存上传文件失败:" + e.getMessage());
        }

        HardenJob job = new HardenJob();
        job.setJobNo(jobNo);
        job.setTenantCode(tenantCode);
        job.setSourceName(sourceName == null ? "upload.jar" : sourceName);
        job.setSourceSize(size);
        job.setTechniques(String.join(",", techs.stream().map(Enum::name).toList()));
        job.setBindLicense(bindLicense);
        job.setStatus("QUEUED");
        job.setCreatedAt(LocalDateTime.now());
        jobRepo.save(job);
        audit.log(null, "HARDEN_SUBMIT", jobNo + " · " + job.getTechniques() + " · " + job.getSourceName());

        final Long id = job.getId();
        pool.submit(() -> run(id, techs, bindLicense, passphrase, encryptPrefix, dir, src));
        return job;
    }

    private List<HardenTechnique> defaultTechs(HardenConfig cfg) {
        List<HardenTechnique> l = new ArrayList<>();
        if (cfg.isObfuscate()) l.add(HardenTechnique.OBFUSCATE);
        if (cfg.isEncryptBind()) l.add(HardenTechnique.ENCRYPT_BIND);
        if (cfg.isFatjarEncrypt()) l.add(HardenTechnique.FATJAR_ENCRYPT);
        return l;
    }

    private void run(Long id, List<HardenTechnique> techs, String bindLicense, String passphrase,
                     String encryptPrefix, Path dir, Path src) {
        HardenJob job = jobRepo.findById(id).orElse(null);
        if (job == null) return;
        job.setStatus("RUNNING");
        jobRepo.save(job);
        try {
            HardenProvider.HardenContext ctx = new HardenProvider.HardenContext();
            ctx.encryptPrefix = encryptPrefix;
            ctx.passphrase = passphrase;
            ctx.runtimeClasses = runtimeClasses();
            if (bindLicense != null && !bindLicense.isBlank()) {
                ctx.licenseLic = licenseService.downloadLic(bindLicense);   // 取该 License 的 .lic 原文用于派生密钥
            }

            Path cur = src;
            StringBuilder msg = new StringBuilder();
            int step = 0;
            for (HardenTechnique t : techs) {
                Path next = dir.resolve("step" + (++step) + ".jar");
                String summary = providers.get(t).apply(cur, next, ctx);
                msg.append("[").append(t).append("] ").append(summary).append("; ");
                cur = next;
            }
            Path outJar = dir.resolve("out.jar");
            Files.copy(cur, outJar, StandardCopyOption.REPLACE_EXISTING);

            job.setOutSize(Files.size(outJar));
            job.setStatus("DONE");
            job.setMessage(trim(msg.toString()));
            job.setFinishedAt(LocalDateTime.now());
            jobRepo.save(job);
            audit.log(null, "HARDEN_DONE", job.getJobNo() + " 加固完成");
        } catch (Throwable e) {   // 含 Error(如 ProGuard 链路 NoClassDefFound),避免 worker 静默挂死
            log.warn("[harden] 任务 {} 失败:{}", job.getJobNo(), e.getMessage());
            job.setStatus("FAILED");
            job.setMessage(trim(rootMsg(e)));
            job.setFinishedAt(LocalDateTime.now());
            jobRepo.save(job);
            audit.log(null, "HARDEN_FAILED", job.getJobNo() + " 失败:" + rootMsg(e));
        }
    }

    /** 产物路径(供下载);未完成抛错。 */
    public Path artifact(Long id) {
        HardenJob job = get(id);
        if (!"DONE".equals(job.getStatus())) throw ApiException.badRequest("任务未完成,暂无产物");
        Path out = Paths.get(props.getHarden().getWorkDir(), job.getJobNo(), "out.jar");
        if (!Files.exists(out)) throw ApiException.notFound("产物不存在(可能已清理)");
        return out;
    }

    private Map<String, byte[]> runtimeClasses() throws Exception {
        Map<String, byte[]> m = new LinkedHashMap<>();
        for (String cn : RUNTIME_CLASSES) {
            String path = "/" + cn.replace('.', '/') + ".class";
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("缺少运行期注入类:" + cn);
                m.put(cn, in.readAllBytes());
            }
        }
        return m;
    }

    private String rootMsg(Throwable e) {
        Throwable c = e; while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }

    private String trim(String s) { return s.length() > 990 ? s.substring(0, 990) + "…" : s; }
}
