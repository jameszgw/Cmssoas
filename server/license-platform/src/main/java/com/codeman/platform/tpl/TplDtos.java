package com.codeman.platform.tpl;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 模板资产管理的请求/响应 DTO。 */
public final class TplDtos {
    private TplDtos() {}

    public record CreateRequest(
            @NotBlank String name,
            String productCode,
            String tenantCode,
            String tags,
            @NotBlank String content
    ) {}

    public record UpdateRequest(String name, String tenantCode, String tags, String content) {}

    public record NoteRequest(String note) {}

    public record TemplateView(
            String code, String name, String productCode, String tenantCode, String tags,
            String status, int currentVersion, boolean hasDraftChanges, int useCount,
            String createdBy, String createdAt, String updatedAt
    ) {
        public static TemplateView from(PrintTemplate t) {
            boolean dirty = t.getDraftContent() != null
                    && !t.getDraftContent().equals(t.getContent());
            return new TemplateView(t.getCode(), t.getName(), t.getProductCode(), t.getTenantCode(),
                    t.getTags(), t.getStatus(), t.getCurrentVersion(), dirty, t.getUseCount(),
                    t.getCreatedBy(), t.getCreatedAt().toString(), t.getUpdatedAt().toString());
        }
    }

    public record TemplateDetail(
            TemplateView meta, String content, String draftContent
    ) {}

    public record VersionView(
            int version, String status, String hash,
            String submittedBy, String submitNote, String reviewedBy, String reviewNote,
            String createdAt, String reviewedAt
    ) {
        public static VersionView from(PrintTemplateVersion v) {
            return new VersionView(v.getVersion(), v.getStatus(), v.getHash(),
                    v.getSubmittedBy(), v.getSubmitNote(), v.getReviewedBy(), v.getReviewNote(),
                    v.getCreatedAt().toString(),
                    v.getReviewedAt() == null ? null : v.getReviewedAt().toString());
        }
    }

    public record GalleryKeyView(String tenantCode, String galleryKey, boolean enabled, String updatedAt) {
        public static GalleryKeyView from(TemplateGalleryKey k) {
            return new GalleryKeyView(k.getTenantCode(), k.getGalleryKey(), k.isEnabled(),
                    k.getUpdatedAt().toString());
        }
    }

    public record VersionList(List<VersionView> versions) {}
}
