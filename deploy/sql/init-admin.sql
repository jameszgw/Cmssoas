-- ============================================================
-- CODEMAN 系统初始化数据(超管账号 + 全量授权)
-- 适用 PostgreSQL / MySQL 5.7+/8.0(纯 SQL 初始化场景)。
-- 说明:应用首次启动时 DataInitializer 会自动完成同样的工作(幂等);
--       仅在"只建库、不启动应用"时才需要手工执行本脚本。
-- 前置:已先执行 schema-postgresql.sql 或 schema-mysql.sql(含 ops_role 种子)。
-- 初始账号:admin / 8888(BCrypt;登录后请尽快改密)。
-- ============================================================

-- 1) 超级管理员账号 admin / 8888
INSERT INTO ops_user(username, password_hash, role_id, status, must_change_pwd, created_at)
SELECT 'admin',
       '$2a$10$.zH/lQd0NFy/O5aBvkjT5eD1x4agru8yyGk3fuIHNbKD3F5lFHoQ6',
       r.id, 'ACTIVE', FALSE, CURRENT_TIMESTAMP
FROM ops_role r
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM ops_user u WHERE u.username = 'admin');

-- 2) 超级管理员 → 全部权限点 FULL(随权限新增自动覆盖,执行即补齐)
INSERT INTO ops_role_permission(role_id, perm_code, mode)
SELECT r.id, p.code, 'FULL'
FROM ops_role r
CROSS JOIN permission p
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM ops_role_permission rp
      WHERE rp.role_id = r.id AND rp.perm_code = p.code);

-- 3) (可选) 只读角色 VIEWER → 各菜单 + *:view 只读
INSERT INTO ops_role_permission(role_id, perm_code, mode)
SELECT r.id, p.code, 'VIEW'
FROM ops_role r
CROSS JOIN permission p
WHERE r.code = 'VIEWER'
  AND (p.type = 'MENU' OR p.code LIKE '%:view')
  AND NOT EXISTS (
      SELECT 1 FROM ops_role_permission rp
      WHERE rp.role_id = r.id AND rp.perm_code = p.code);
