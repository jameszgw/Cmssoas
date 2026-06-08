-- [MySQL 方言] 由 db/migration/V16__tenant_portal.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 租户自助门户:为租户开通门户访问(租户编号 + 访问码),最终客户登录后只读查看本租户数据。
CREATE TABLE tenant_portal (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(32) NOT NULL,
    access_code VARCHAR(32) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT 1,
    created_at  DATETIME   NOT NULL,
    updated_at  DATETIME   NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE UNIQUE INDEX uk_portal_tenant ON tenant_portal(tenant_code);

INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('tenant:portal','开通/管理自助门户','tenant','ACTION',5);
