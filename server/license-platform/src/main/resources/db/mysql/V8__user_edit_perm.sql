-- [MySQL 方言] 由 db/migration/V8__user_edit_perm.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 新增「编辑用户」权限点（用户管理）
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('user:edit','编辑用户','system','ACTION',4);
