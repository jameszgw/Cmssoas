-- [MySQL 方言] 由 db/migration/V15__customer.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 统一客户主数据(Customer)。License/账单/合同/订阅/支付按 customer 名称聚合成「客户360」。
CREATE TABLE customer (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL,    -- 客户编号(自动)
    name        VARCHAR(128) NOT NULL,    -- 客户名称(与各业务实体 customer 字段对应)
    tenant_code VARCHAR(32),              -- 可选:关联租户
    contact     VARCHAR(64),              -- 联系人
    email       VARCHAR(128),
    phone       VARCHAR(32),
    industry    VARCHAR(64),
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / INACTIVE
    note        VARCHAR(512),
    created_at  DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE UNIQUE INDEX uk_customer_code ON customer(code);
CREATE INDEX idx_customer_name ON customer(name);

INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('customer','客户管理',NULL,'MENU',8),
 ('customer:view','查看客户/客户360','customer','ACTION',1),
 ('customer:edit','新建/编辑客户','customer','ACTION',2);
