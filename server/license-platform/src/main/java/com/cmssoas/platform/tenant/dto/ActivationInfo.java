package com.cmssoas.platform.tenant.dto;

/** 激活页所需信息（凭 token 查询）。 */
public record ActivationInfo(
        String tenantName,
        String tenantCode,
        String email,
        String version,
        boolean valid,
        String message
) {}
