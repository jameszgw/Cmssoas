package com.cmssoas.platform.rbac.service;

import com.cmssoas.platform.rbac.domain.OpsRole;
import com.cmssoas.platform.rbac.domain.OpsUser;
import com.cmssoas.platform.rbac.domain.Permission;
import com.cmssoas.platform.rbac.domain.RolePermission;
import com.cmssoas.platform.rbac.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 初始化：SUPER_ADMIN 全量权限 + 初始账号 admin/8888。 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permRepo;
    private final OpsRoleRepository roleRepo;
    private final RolePermissionRepository rolePermRepo;
    private final OpsUserRepository userRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(PermissionRepository permRepo, OpsRoleRepository roleRepo,
                           RolePermissionRepository rolePermRepo, OpsUserRepository userRepo,
                           PasswordEncoder encoder) {
        this.permRepo = permRepo;
        this.roleRepo = roleRepo;
        this.rolePermRepo = rolePermRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        OpsRole superRole = roleRepo.findByCode("SUPER_ADMIN").orElse(null);
        if (superRole == null) return;

        // SUPER_ADMIN -> 全部权限 FULL
        if (rolePermRepo.findByRoleId(superRole.getId()).isEmpty()) {
            for (Permission p : permRepo.findAll()) {
                rolePermRepo.save(new RolePermission(superRole.getId(), p.getCode(), "FULL"));
            }
            log.info("[init] SUPER_ADMIN 已授予全部权限");
        }

        // VIEWER -> 各菜单 + *:view 只读
        roleRepo.findByCode("VIEWER").ifPresent(viewer -> {
            if (rolePermRepo.findByRoleId(viewer.getId()).isEmpty()) {
                for (Permission p : permRepo.findAll()) {
                    if ("MENU".equals(p.getType()) || p.getCode().endsWith(":view")) {
                        rolePermRepo.save(new RolePermission(viewer.getId(), p.getCode(), "VIEW"));
                    }
                }
                log.info("[init] VIEWER 已授予只读权限");
            }
        });

        // 初始账号 admin / 8888
        if (userRepo.findByUsername("admin").isEmpty()) {
            OpsUser u = new OpsUser();
            u.setUsername("admin");
            u.setPasswordHash(encoder.encode("8888"));
            u.setRoleId(superRole.getId());
            u.setStatus("ACTIVE");
            u.setMustChangePwd(false);
            u.setCreatedAt(LocalDateTime.now());
            userRepo.save(u);
            log.info("[init] 初始账号已创建：admin / 8888（超级管理员）");
        }
    }
}
