package com.codeman.platform.rbac.web;

import com.codeman.platform.common.ApiException;
import com.codeman.platform.rbac.dto.RbacDtos.*;
import com.codeman.platform.rbac.dto.UserDtos.ChangePwdRequest;
import com.codeman.platform.rbac.service.AuthService;
import com.codeman.platform.rbac.service.CurrentUser;
import com.codeman.platform.rbac.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserService userService;

    public AuthController(AuthService auth, UserService userService) {
        this.auth = auth;
        this.userService = userService;
    }

    @PostMapping("/login")
    public AuthResult login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public AuthResult me() {
        CurrentUser.Ctx c = CurrentUser.get();
        if (c == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        return auth.me(c.uid());
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@Valid @RequestBody ChangePwdRequest req) {
        CurrentUser.Ctx c = CurrentUser.get();
        if (c == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        userService.changePassword(c.uid(), req);
        return Map.of("success", true, "message", "密码已修改");
    }
}
