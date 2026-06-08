package com.codeman.platform.rbac.web;

import com.codeman.platform.rbac.dto.UserDtos.*;
import com.codeman.platform.rbac.service.RequirePerm;
import com.codeman.platform.rbac.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("user:view")
    public List<UserView> list() {
        return service.list();
    }

    @PostMapping
    @RequirePerm("user:edit")
    public UserView create(@Valid @RequestBody CreateUserRequest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/reset-password")
    @RequirePerm("user:edit")
    public Map<String, Object> reset(@PathVariable Long id, @RequestBody(required = false) ResetPwdRequest req) {
        service.resetPassword(id, req == null ? new ResetPwdRequest(null) : req);
        return Map.of("ok", true, "defaultPassword", UserService.DEFAULT_PWD);
    }

    @PostMapping("/{id}/toggle")
    @RequirePerm("user:edit")
    public Map<String, Object> toggle(@PathVariable Long id) {
        service.toggle(id);
        return Map.of("ok", true);
    }
}
