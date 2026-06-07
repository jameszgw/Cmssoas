-- [MySQL 方言] 由 db/migration/V16__tenant_portal.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 租户自助门户:为租户开通门户访问(租户编号 + 访问码),最终客户登录后只读查看本租户数据。
CREATE TABLE tenant_portal (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(32) NOT NULL,
    access_code VARCHAR(32) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT 1,
    created_at  DATETIME   NOT NULL,
    updated_at  DATETIME   NOT NULL
);
CREATE UNIQUE INDEX uk_portal_tenant ON tenant_portal(tenant_code);

INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('tenant:portal','开通/管理自助门户','tenant','ACTION',5);
