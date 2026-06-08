package com.codeman.platform.tenant.dto;

/** 激活页所需信息（凭 token 查询），含 MFA 绑定所需的 otpauth 链接与密钥。 */
public record ActivationInfo(
        String tenantName,
        String tenantCode,
        String email,
        String version,
        String mfaOtpauthUri,
        String mfaSecret,
        boolean valid,
        String message
) {}
