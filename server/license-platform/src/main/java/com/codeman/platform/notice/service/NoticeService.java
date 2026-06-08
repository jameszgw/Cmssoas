package com.codeman.platform.notice.service;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.common.AuditWriter;
import com.codeman.platform.notice.domain.Notice;
import com.codeman.platform.notice.repo.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 须知服务：草稿编辑 + 版本化发布（发布即归档同类型旧版，版本号自增，内容不可再改）。 */
@Service
public class NoticeService {

    private final NoticeRepository repo;
    private final AuditWriter audit;

    public NoticeService(NoticeRepository repo, AuditWriter audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public List<Notice> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public Notice get(Long id) {
        return repo.findById(id).orElseThrow(() -> ApiException.notFound("须知不存在"));
    }

    /** 取某类型当前生效版本（公开页用）。 */
    public Notice active(String type) {
        return repo.findFirstByTypeAndStatusOrderByVersionDesc(type, "PUBLISHED").orElse(null);
    }

    @Transactional
    public Notice create(String type, String title, String contentHtml, boolean forceAck) {
        Notice n = new Notice();
        n.setType(type);
        n.setTitle(title);
        n.setContentHtml(contentHtml);
        n.setForceAck(forceAck);
        n.setStatus("DRAFT");
        // 版本号在已有同类型最高版本基础上 +1（草稿即占号，发布时定稿）
        int base = repo.findFirstByTypeOrderByVersionDesc(type).map(Notice::getVersion).orElse(0);
        n.setVersion(base + 1);
        n.setCreatedAt(LocalDateTime.now());
        repo.save(n);
        audit.log(null, "NOTICE_CREATE", type + " · v" + n.getVersion() + " · " + title);
        return n;
    }

    @Transactional
    public Notice update(Long id, String title, String contentHtml, boolean forceAck) {
        Notice n = get(id);
        if (!"DRAFT".equals(n.getStatus())) throw ApiException.badRequest("仅草稿可编辑；已发布版本不可变更，请新建版本");
        n.setTitle(title);
        n.setContentHtml(contentHtml);
        n.setForceAck(forceAck);
        repo.save(n);
        audit.log(null, "NOTICE_UPDATE", "须知#" + id + " 已更新");
        return n;
    }

    @Transactional
    public Notice publish(Long id) {
        Notice n = get(id);
        if (!"DRAFT".equals(n.getStatus())) throw ApiException.badRequest("仅草稿可发布");
        // 归档同类型旧的已发布版本
        repo.findFirstByTypeAndStatusOrderByVersionDesc(n.getType(), "PUBLISHED").ifPresent(old -> {
            old.setStatus("ARCHIVED");
            repo.save(old);
        });
        n.setStatus("PUBLISHED");
        n.setEffectiveAt(LocalDateTime.now());
        repo.save(n);
        audit.log(null, "NOTICE_PUBLISH", n.getType() + " v" + n.getVersion() + " 已发布生效");
        return n;
    }
}
