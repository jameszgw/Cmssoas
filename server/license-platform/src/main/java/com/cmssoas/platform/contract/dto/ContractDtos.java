package com.cmssoas.platform.contract.dto;

import java.util.List;

/** 合同相关请求体。 */
public final class ContractDtos {
    private ContractDtos() {}

    public record TemplateReq(String name, String contentHtml, String variables) {}

    public record PartyReq(String name, String partyRole, String email, String phone) {}

    public record CreateReq(
            Long templateId, String tenantCode, String customer, Long subscriptionId,
            String planCode, String title, String contentHtml, Integer amount,
            List<PartyReq> parties) {}
}
