-- [MySQL 方言] 由 db/migration/V7__rbac.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- RBAC：权限树 / 角色 / 角色-权限(多态 mode) / 运营用户

CREATE TABLE permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(48) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    parent_code VARCHAR(48),
    type        VARCHAR(16) NOT NULL,   -- MENU / ACTION
    sort        INT NOT NULL DEFAULT 0
);
CREATE TABLE ops_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(128)
);
CREATE TABLE ops_role_permission (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id   BIGINT NOT NULL,
    perm_code VARCHAR(48) NOT NULL,
    mode      VARCHAR(16) NOT NULL,   -- NONE / VIEW / EDIT / FULL
    CONSTRAINT uk_role_perm UNIQUE (role_id, perm_code)
);
CREATE TABLE ops_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    role_id         BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL,
    must_change_pwd BOOLEAN NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL
);

-- 权限树种子
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('overview','运营总览',NULL,'MENU',1),
 ('tenant','租户管理',NULL,'MENU',2),
 ('tenant:view','查看租户','tenant','ACTION',1),
 ('tenant:onboard','开通租户','tenant','ACTION',2),
 ('tenant:renew','续期租户','tenant','ACTION',3),
 ('license','License 授权',NULL,'MENU',3),
 ('license:view','查看 License','license','ACTION',1),
 ('license:issue','签发 License','license','ACTION',2),
 ('license:renew','续期 License','license','ACTION',3),
 ('license:revoke','吊销 License','license','ACTION',4),
 ('license:download','下载 .lic','license','ACTION',5),
 ('online','在线监控',NULL,'MENU',4),
 ('online:view','查看在线实例','online','ACTION',1),
 ('catalog','产品与版本',NULL,'MENU',5),
 ('catalog:view','查看产品矩阵','catalog','ACTION',1),
 ('plan','套餐订阅',NULL,'MENU',6),
 ('plan:view','查看套餐','plan','ACTION',1),
 ('plan:subscribe','创建订阅','plan','ACTION',2),
 ('audit','审计追溯',NULL,'MENU',7),
 ('audit:view','查看审计','audit','ACTION',1),
 ('system','系统管理',NULL,'MENU',8),
 ('role:view','查看角色权限','system','ACTION',1),
 ('role:edit','编辑角色权限','system','ACTION',2),
 ('user:view','查看用户','system','ACTION',3);

-- 角色种子（SUPER_ADMIN 的权限映射由 DataInitializer 填充为 FULL）
INSERT INTO ops_role(code,name,description) VALUES
 ('SUPER_ADMIN','超级管理员','拥有全部权限'),
 ('OPERATOR','运营专员','日常运营操作'),
 ('VIEWER','只读访客','仅查看');
