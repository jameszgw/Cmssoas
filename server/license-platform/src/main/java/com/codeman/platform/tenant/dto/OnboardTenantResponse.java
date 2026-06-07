package com.codeman.platform.tenant.dto;

/** 开通结果（与前端 OnboardResult 对应：{ code, emailSent, email }）。 */
public record OnboardTenantResponse(
        String code,
        boolean emailSent,
        String email
) {}
