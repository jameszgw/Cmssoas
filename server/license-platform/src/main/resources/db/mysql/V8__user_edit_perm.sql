-- [MySQL 方言] 由 db/migration/V8__user_edit_perm.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 新增「编辑用户」权限点（用户管理）
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('user:edit','编辑用户','system','ACTION',4);
