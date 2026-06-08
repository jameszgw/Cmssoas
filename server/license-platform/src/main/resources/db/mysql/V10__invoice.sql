-- [MySQL 方言] 由 db/migration/V10__invoice.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 计费账单 / 开票
CREATE TABLE invoice (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no      VARCHAR(40),
    tenant_code     VARCHAR(32)  NOT NULL,
    customer        VARCHAR(128) NOT NULL,
    subscription_id BIGINT,
    plan_code       VARCHAR(32),
    type            VARCHAR(16)  NOT NULL,   -- SUBSCRIBE / CHANGE / RENEW
    amount          INT          NOT NULL,   -- 金额（元）
    currency        VARCHAR(8)   NOT NULL DEFAULT 'CNY',
    period          VARCHAR(40),
    status          VARCHAR(16)  NOT NULL,   -- PENDING / PAID / INVOICED / VOID
    created_at      DATETIME    NOT NULL,
    paid_at         DATETIME,
    invoiced_at     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_invoice_tenant ON invoice(tenant_code);
CREATE INDEX idx_invoice_status ON invoice(status);

-- 账单权限点
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('billing','计费账单',NULL,'MENU',9),
 ('billing:view','查看账单','billing','ACTION',1),
 ('billing:manage','收款/开票','billing','ACTION',2);
