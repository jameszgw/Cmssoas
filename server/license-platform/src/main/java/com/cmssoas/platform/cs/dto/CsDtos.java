package com.cmssoas.platform.cs.dto;

/** 智能客服请求体。 */
public final class CsDtos {
    private CsDtos() {}

    public record ChatReq(Long conversationId, String question) {}
}
