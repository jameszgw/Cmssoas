package com.codeman.platform.tpl;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.rbac.service.CurrentUser;
import com.codeman.platform.tpl.TplDtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

/**
 * 模板资产管理:模板 CRUD + 审批流(草稿→送审(只读)→审批通过生效/驳回) + 版本留档/回滚 +
 * 按租户模板库密钥。所有状态变更写审计(TPL_*),与 License 审计同一通道,可在 CmPrint 审计查询追溯。
 */
@Service
public class TplService {

    /** 模板状态。 */
    public static final String ST_DRAFT = "DRAFT";
    public static final String ST_PENDING = "PENDING";
    public static final String ST_APPROVED = "APPROVED";
    public static final String ST_DISABLED = "DISABLED";

    /** 平台公共库的密钥归属。 */
    public static final String PUBLIC_TENANT = "PUBLIC";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PrintTemplateRepository tplRepo;
    private final PrintTemplateVersionRepository verRepo;
    private final TemplateGalleryKeyRepository keyRepo;
    private final ObjectMapper mapper;
    private final AuditWriter audit;

    public TplService(PrintTemplateRepository tplRepo, PrintTemplateVersionRepository verRepo,
                      TemplateGalleryKeyRepository keyRepo, ObjectMapper mapper, AuditWriter audit) {
        this.tplRepo = tplRepo;
        this.verRepo = verRepo;
        this.keyRepo = keyRepo;
        this.mapper = mapper;
        this.audit = audit;
    }

    // ---------- 查询 ----------
    public List<TemplateView> list(String status, String keyword) {
        return tplRepo.findAllByOrderByUpdatedAtDesc().stream()
                .filter(t -> status == null || status.isBlank() || t.getStatus().equals(status))
                .filter(t -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String k = keyword.trim().toLowerCase();
                    return t.getName().toLowerCase().contains(k)
                            || t.getCode().toLowerCase().contains(k)
                            || (t.getTenantCode() != null && t.getTenantCode().toLowerCase().contains(k))
                            || (t.getTags() != null && t.getTags().toLowerCase().contains(k));
                })
                .map(TemplateView::from).toList();
    }

    public TemplateDetail detail(String code) {
        PrintTemplate t = require(code);
        return new TemplateDetail(TemplateView.from(t), t.getContent(), t.getDraftContent());
    }

    public List<VersionView> versions(String code) {
        require(code);
        return verRepo.findByTemplateCodeOrderByVersionDesc(code).stream().map(VersionView::from).toList();
    }

    /** 导出生效内容(无生效版时导出草稿);写审计(谁导出了哪个模板)。 */
    public String exportContent(String code) {
        PrintTemplate t = require(code);
        String content = t.getContent() != null ? t.getContent() : t.getDraftContent();
        if (content == null) throw ApiException.badRequest("模板尚无内容可导出");
        audit.log(null, "TPL_EXPORT", t.getCode() + " · " + t.getName());
        return content;
    }

    // ---------- 编辑(草稿) ----------
    @Transactional
    public TemplateView create(CreateRequest r) {
        validateTemplateJson(r.content());
        PrintTemplate t = new PrintTemplate();
        t.setCode(nextCode());
        t.setName(r.name().trim());
        t.setProductCode(orElse(r.productCode(), "CMPRINT"));
        t.setTenantCode(blankToNull(r.tenantCode()));
        t.setTags(blankToNull(r.tags()));
        t.setStatus(ST_DRAFT);
        t.setCurrentVersion(0);
        t.setDraftContent(r.content());
        t.setCreatedBy(operator());
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, "TPL_CREATE", t.getCode() + " · " + t.getName());
        return TemplateView.from(t);
    }

    @Transactional
    public TemplateView update(String code, UpdateRequest r) {
        PrintTemplate t = require(code);
        ensureEditable(t);
        if (r.name() != null && !r.name().isBlank()) t.setName(r.name().trim());
        if (r.tenantCode() != null) t.setTenantCode(blankToNull(r.tenantCode()));
        if (r.tags() != null) t.setTags(blankToNull(r.tags()));
        if (r.content() != null && !r.content().isBlank()) {
            validateTemplateJson(r.content());
            t.setDraftContent(r.content());
        }
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, "TPL_UPDATE", t.getCode() + " · " + t.getName());
        return TemplateView.from(t);
    }

    // ---------- 审批流 ----------
    /** 送审:以当前草稿生成 PENDING 版本,模板转入审批中(只读)。 */
    @Transactional
    public VersionView submit(String code, String note) {
        PrintTemplate t = require(code);
        ensureEditable(t);
        if (t.getDraftContent() == null || t.getDraftContent().isBlank()) {
            throw ApiException.badRequest("草稿为空,无可送审内容");
        }
        if (t.getCurrentVersion() > 0 && t.getDraftContent().equals(t.getContent())) {
            throw ApiException.badRequest("草稿与生效版本一致,无需送审");
        }
        int next = verRepo.findByTemplateCodeOrderByVersionDesc(code).stream()
                .mapToInt(PrintTemplateVersion::getVersion).max().orElse(0) + 1;
        PrintTemplateVersion v = new PrintTemplateVersion();
        v.setTemplateCode(code);
        v.setVersion(next);
        v.setContent(t.getDraftContent());
        v.setHash(fnv(t.getDraftContent()));
        v.setStatus(ST_PENDING);
        v.setSubmittedBy(operator());
        v.setSubmitNote(blankToNull(note));
        v.setCreatedAt(LocalDateTime.now());
        verRepo.save(v);
        t.setStatus(ST_PENDING);
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, "TPL_SUBMIT", code + " v" + next + (note == null || note.isBlank() ? "" : " · " + note));
        return VersionView.from(v);
    }

    /** 审批通过:版本生效(写入 content/currentVersion),模板回到 APPROVED。 */
    @Transactional
    public TemplateView approve(String code, String note) {
        PrintTemplate t = require(code);
        PrintTemplateVersion v = pendingVersion(code);
        v.setStatus(ST_APPROVED);
        v.setReviewedBy(operator());
        v.setReviewNote(blankToNull(note));
        v.setReviewedAt(LocalDateTime.now());
        verRepo.save(v);
        t.setContent(v.getContent());
        t.setCurrentVersion(v.getVersion());
        t.setStatus(ST_APPROVED);
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, "TPL_APPROVE", code + " v" + v.getVersion()
                + (note == null || note.isBlank() ? "" : " · " + note));
        return TemplateView.from(t);
    }

    /** 驳回:版本置 REJECTED,模板回到可编辑(有生效版回 APPROVED,否则回 DRAFT)。 */
    @Transactional
    public TemplateView reject(String code, String note) {
        PrintTemplate t = require(code);
        PrintTemplateVersion v = pendingVersion(code);
        v.setStatus("REJECTED");
        v.setReviewedBy(operator());
        v.setReviewNote(blankToNull(note));
        v.setReviewedAt(LocalDateTime.now());
        verRepo.save(v);
        t.setStatus(t.getCurrentVersion() > 0 ? ST_APPROVED : ST_DRAFT);
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, "TPL_REJECT", code + " v" + v.getVersion()
                + (note == null || note.isBlank() ? "" : " · " + note));
        return TemplateView.from(t);
    }

    /** 回滚:把历史版本内容拉回草稿(走正常送审→审批生效,轨迹完整)。 */
    @Transactional
    public TemplateView rollback(String code, int version) {
        PrintTemplate t = require(code);
        ensureEditable(t);
        PrintTemplateVersion v = verRepo.findByTemplateCodeAndVersion(code, version)
                .orElseThrow(() -> ApiException.notFound("版本不存在:v" + version));
        t.setDraftContent(v.getContent());
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, "TPL_ROLLBACK", code + " 草稿回滚至 v" + version);
        return TemplateView.from(t);
    }

    @Transactional
    public TemplateView setDisabled(String code, boolean disabled) {
        PrintTemplate t = require(code);
        if (disabled) {
            if (ST_PENDING.equals(t.getStatus())) throw ApiException.badRequest("审批中的模板不可下架");
            t.setStatus(ST_DISABLED);
        } else {
            t.setStatus(t.getCurrentVersion() > 0 ? ST_APPROVED : ST_DRAFT);
        }
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        audit.log(null, disabled ? "TPL_DISABLE" : "TPL_ENABLE", code + " · " + t.getName());
        return TemplateView.from(t);
    }

    @Transactional
    public void delete(String code) {
        PrintTemplate t = require(code);
        verRepo.deleteByTemplateCode(code);
        tplRepo.delete(t);
        audit.log(null, "TPL_DELETE", code + " · " + t.getName());
    }

    // ---------- 模板库密钥 ----------
    public List<GalleryKeyView> listKeys() {
        return keyRepo.findAll().stream().map(GalleryKeyView::from).toList();
    }

    /** 取(无则创建)某租户的模板库密钥;tenant='PUBLIC' 为平台公共库。 */
    @Transactional
    public GalleryKeyView ensureKey(String tenantCode) {
        String tc = orElse(tenantCode, PUBLIC_TENANT);
        TemplateGalleryKey k = keyRepo.findByTenantCode(tc).orElseGet(() -> {
            TemplateGalleryKey nk = new TemplateGalleryKey();
            nk.setTenantCode(tc);
            nk.setGalleryKey(randomKey());
            nk.setEnabled(true);
            nk.setUpdatedAt(LocalDateTime.now());
            return keyRepo.save(nk);
        });
        return GalleryKeyView.from(k);
    }

    /** 重置密钥(旧地址立即失效)。 */
    @Transactional
    public GalleryKeyView resetKey(String tenantCode) {
        String tc = orElse(tenantCode, PUBLIC_TENANT);
        TemplateGalleryKey k = keyRepo.findByTenantCode(tc)
                .orElseThrow(() -> ApiException.notFound("该租户尚未开通模板库:" + tc));
        k.setGalleryKey(randomKey());
        k.setUpdatedAt(LocalDateTime.now());
        keyRepo.save(k);
        audit.log(null, "TPL_GALLERY_KEY_RESET", tc);
        return GalleryKeyView.from(k);
    }

    // ---------- 云端模板库(CmPrint 设计器契约) ----------
    /** 按密钥取可见模板:APPROVED 且 (平台公共 或 属于该租户);PUBLIC 密钥仅见公共。 */
    public List<Map<String, Object>> galleryList(String galleryKey) {
        TemplateGalleryKey k = requireKey(galleryKey);
        return tplRepo.findByStatusOrderByUpdatedAtDesc(ST_APPROVED).stream()
                .filter(t -> visibleTo(t, k))
                .map(this::galleryNode)
                .toList();
    }

    /** 设计器上传共享:落为该租户的 PENDING 模板(审批通过后方进入列表)。 */
    @Transactional
    public Map<String, Object> gallerySave(String galleryKey, JsonNode body) {
        TemplateGalleryKey k = requireKey(galleryKey);
        JsonNode template = body == null ? null : body.get("template");
        if (template == null || !template.has("panels")) {
            throw ApiException.badRequest("缺少 template.panels");
        }
        String name = body.hasNonNull("name") ? body.get("name").asText() : "未命名模板";
        String owner = body.hasNonNull("owner") ? body.get("owner").asText()
                : body.hasNonNull("author") ? body.get("author").asText() : "gallery";

        PrintTemplate t = new PrintTemplate();
        t.setCode(nextCode());
        t.setName(name);
        t.setProductCode("CMPRINT");
        t.setTenantCode(PUBLIC_TENANT.equals(k.getTenantCode()) ? null : k.getTenantCode());
        t.setStatus(ST_DRAFT);
        t.setCurrentVersion(0);
        t.setDraftContent(template.toString());
        t.setCreatedBy(owner);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        tplRepo.save(t);
        // 直接送审(运营侧审批通过后才出现在模板库)
        int next = 1;
        PrintTemplateVersion v = new PrintTemplateVersion();
        v.setTemplateCode(t.getCode());
        v.setVersion(next);
        v.setContent(t.getDraftContent());
        v.setHash(fnv(t.getDraftContent()));
        v.setStatus(ST_PENDING);
        v.setSubmittedBy(owner);
        v.setSubmitNote("模板库上传");
        v.setCreatedAt(LocalDateTime.now());
        verRepo.save(v);
        t.setStatus(ST_PENDING);
        tplRepo.save(t);
        audit.log(null, "TPL_GALLERY_SAVE", t.getCode() + " · " + name + " · " + owner
                + " @" + k.getTenantCode());
        Map<String, Object> node = galleryNode(t);
        node.put("pending", true);
        return node;
    }

    @Transactional
    public Map<String, Object> galleryUse(String galleryKey, String code) {
        TemplateGalleryKey k = requireKey(galleryKey);
        PrintTemplate t = require(code);
        if (!ST_APPROVED.equals(t.getStatus()) || !visibleTo(t, k)) {
            throw ApiException.notFound("模板不可用:" + code);
        }
        t.setUseCount(t.getUseCount() + 1);
        tplRepo.save(t);
        return galleryNode(t);
    }

    /** 设计器删除:仅允许属主删除本租户**尚未生效**的上传(生效模板的删除走运营端)。 */
    @Transactional
    public void galleryRemove(String galleryKey, String code, String owner) {
        TemplateGalleryKey k = requireKey(galleryKey);
        PrintTemplate t = require(code);
        boolean sameTenant = Objects.equals(t.getTenantCode(),
                PUBLIC_TENANT.equals(k.getTenantCode()) ? null : k.getTenantCode());
        if (!sameTenant || owner == null || !owner.equals(t.getCreatedBy())) {
            throw ApiException.badRequest("仅模板属主可删除");
        }
        if (ST_APPROVED.equals(t.getStatus())) {
            throw ApiException.badRequest("已生效模板请联系运营下架/删除");
        }
        verRepo.deleteByTemplateCode(code);
        tplRepo.delete(t);
        audit.log(null, "TPL_DELETE", code + " · 模板库属主删除 · " + owner);
    }

    // ---------- 内部 ----------
    private Map<String, Object> galleryNode(PrintTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getCode());
        m.put("name", t.getName());
        try {
            m.put("template", mapper.readTree(
                    t.getContent() != null ? t.getContent() : t.getDraftContent()));
        } catch (Exception e) {
            m.put("template", null);
        }
        if (t.getTags() != null) m.put("tags", Arrays.asList(t.getTags().split(",")));
        m.put("version", Math.max(t.getCurrentVersion(), 1));
        m.put("author", t.getCreatedBy());
        m.put("owner", t.getCreatedBy());
        m.put("useCount", t.getUseCount());
        m.put("cloud", true);
        return m;
    }

    private boolean visibleTo(PrintTemplate t, TemplateGalleryKey k) {
        if (t.getTenantCode() == null) return true;                      // 平台公共
        if (PUBLIC_TENANT.equals(k.getTenantCode())) return false;       // 公共密钥只见公共
        return t.getTenantCode().equals(k.getTenantCode());              // 租户密钥见本租户
    }

    private TemplateGalleryKey requireKey(String galleryKey) {
        TemplateGalleryKey k = keyRepo.findByGalleryKey(galleryKey == null ? "" : galleryKey.trim())
                .orElseThrow(() -> ApiException.notFound("模板库地址无效"));
        if (!k.isEnabled()) throw ApiException.badRequest("模板库已停用");
        return k;
    }

    private PrintTemplate require(String code) {
        return tplRepo.findByCode(code).orElseThrow(() -> ApiException.notFound("模板不存在:" + code));
    }

    private PrintTemplateVersion pendingVersion(String code) {
        return verRepo.findByTemplateCodeOrderByVersionDesc(code).stream()
                .filter(v -> ST_PENDING.equals(v.getStatus()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("没有待审批的版本"));
    }

    private void ensureEditable(PrintTemplate t) {
        if (ST_PENDING.equals(t.getStatus())) {
            throw ApiException.badRequest("模板审批中(只读),请先完成审批");
        }
    }

    private void validateTemplateJson(String content) {
        try {
            JsonNode n = mapper.readTree(content);
            if (!n.has("panels") || !n.get("panels").isArray()) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw ApiException.badRequest("模板内容须为含 panels 数组的 JSON");
        }
    }

    private String nextCode() {
        int year = Year.now().getValue();
        long seq = tplRepo.count() + 1;
        String code;
        do {
            code = String.format("TPL-%d-%04d", year, seq);
            seq++;
        } while (tplRepo.findByCode(code).isPresent());
        return code;
    }

    private String operator() {
        CurrentUser.Ctx c = CurrentUser.get();
        return c != null ? c.username() : "system";
    }

    /** FNV-1a 内容哈希(版本去重/对比用)。 */
    static String fnv(String s) {
        int h = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return String.format("%08x", h);
    }

    private static String randomKey() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String orElse(String v, String def) { return (v == null || v.isBlank()) ? def : v; }

    private static String blankToNull(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}
