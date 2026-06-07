# 数据库脚本说明(deploy/sql)

| 文件 | 用途 | 适用 |
|---|---|---|
| `schema-postgresql.sql` | **全量表结构 + 系统种子**(权限/角色/套餐等),PostgreSQL/H2 | PostgreSQL、H2 |
| `schema-mysql.sql` | **全量表结构 + 系统种子**,MySQL 方言(`AUTO_INCREMENT`/`DATETIME`/每表 `utf8mb4`) | MySQL 5.7 / 8.0 |
| `init-admin.sql` | 超管账号 **admin/8888**(BCrypt) + 超管全量授权 + VIEWER 只读授权(幂等) | 两者 |
| `test-data.sql` | 演示/测试数据(租户/客户/产品/套餐/须知),**生产勿用** | 两者 |
| `generate-schema.sh` | 由 Flyway 迁移合并**重新生成**上面两个 schema 文件 | 维护用 |

## 执行顺序
1. 建库(MySQL 需 utf8mb4):
   ```sql
   -- PostgreSQL: CREATE DATABASE cmssoas;
   -- MySQL:      CREATE DATABASE cmssoas CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
2. `schema-*.sql`(对应方言)— 建表 + 系统种子。
3. `init-admin.sql`(可选)— 创建超管与授权。**应用首次启动也会自动完成,故走应用建表时可跳过。**
4. `test-data.sql`(可选)— 演示数据。

## 重要说明
- **schema 脚本由 `generate-schema.sh` 自动生成**(合并 `server/.../db/migration` 与 `db/mysql` 的 Flyway 迁移),
  请勿手改;改库结构请改迁移脚本后重新生成。
- 走"应用自动建表(Flyway)"时**无需**手工执行本目录脚本;本目录主要用于纯 SQL 初始化、评审、离线交付。
- 初始密码 admin/8888,登录后请尽快修改。
- 已在 H2 的 PostgreSQL 与 MySQL 兼容模式下校验:schema + init-admin + test-data 可干净顺序应用;
  MySQL 5.7/8.0 真机迁移由 CI 持续验证。
