package com.codeman.platform.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 管理员激活并设置密码；mfaCode 可选（提供则校验并绑定 MFA）。 */
public record ActivateRequest(
        @NotBlank @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间") String password,
        String mfaCode
) {}
