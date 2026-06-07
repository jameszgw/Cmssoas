package com.cmssoas.platform.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record UserView(Long id, String username, String roleCode, String roleName,
                           String status, boolean mustChangePwd, String createdAt) {}

    public record CreateUserRequest(
            @NotBlank String username,
            @NotNull Long roleId,
            String password
    ) {}

    public record ResetPwdRequest(String password) {}

    public record ChangePwdRequest(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 6, max = 64, message = "新密码长度需在 6-64 位之间") String newPassword
    ) {}
}
