package com.cmssoas.platform.notice.dto;

/** 须知相关请求体。 */
public final class NoticeDtos {
    private NoticeDtos() {}

    public record CreateReq(String type, String title, String contentHtml, boolean forceAck) {}
    public record UpdateReq(String title, String contentHtml, boolean forceAck) {}
    public record ConsentReq(Long noticeId, String tenantCode, String subject, String channel) {}
}
