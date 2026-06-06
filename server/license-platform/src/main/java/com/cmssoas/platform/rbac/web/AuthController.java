package com.cmssoas.platform.rbac.web;

import com.cmssoas.platform.common.ApiException;
import com.cmssoas.platform.rbac.dto.RbacDtos.*;
import com.cmssoas.platform.rbac.service.AuthService;
import com.cmssoas.platform.rbac.service.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
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
}
