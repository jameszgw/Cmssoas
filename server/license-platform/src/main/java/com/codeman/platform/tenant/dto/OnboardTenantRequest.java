package com.codeman.platform.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 开通租户请求（与前端 OnboardTenantDialog 字段对应）。 */
public record OnboardTenantRequest(
        @NotBlank(message = "请输入租户名称") String name,
        String plan,
        String version,
        @NotBlank(message = "请输入管理员邮箱")
        @Email(message = "请输入有效的管理员邮箱") String email,
        String isolation,
        String mode
) {}
