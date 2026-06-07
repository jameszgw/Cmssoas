package com.codeman.platform.rbac.web;

import com.codeman.platform.rbac.dto.RbacDtos.*;
import com.codeman.platform.rbac.service.RbacService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    private final RbacService rbac;

    public RbacController(RbacService rbac) {
        this.rbac = rbac;
    }

    @GetMapping("/permissions")
    public List<PermNode> permissions() {
        return rbac.permissionTree();
    }

    @GetMapping("/roles")
    public List<RoleView> roles() {
        return rbac.roles();
    }

    @GetMapping("/roles/{id}")
    public RoleDetail role(@PathVariable Long id) {
        return rbac.roleDetail(id);
    }

    @PutMapping("/roles/{id}/permissions")
    public RoleDetail setPermissions(@PathVariable Long id, @RequestBody List<PermItem> items) {
        rbac.setRolePermissions(id, items);
        return rbac.roleDetail(id);
    }
}
