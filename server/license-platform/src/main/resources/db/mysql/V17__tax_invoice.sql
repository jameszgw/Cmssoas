-- [MySQL 方言] 由 db/migration/V17__tax_invoice.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 正规税务发票(电子发票)。通用 EInvoiceProvider 抽象,默认沙箱;预留航信/百望/税务 API。
CREATE TABLE tax_invoice (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id  BIGINT       NOT NULL,    -- 关联计费账单
    title       VARCHAR(160) NOT NULL,    -- 发票抬头(购方名称)
    tax_no      VARCHAR(32),              -- 购方税号
    type        VARCHAR(16)  NOT NULL,    -- NORMAL(普票) / SPECIAL(专票)
    email       VARCHAR(128),             -- 收票邮箱
    amount      INT          NOT NULL,
    status      VARCHAR(16)  NOT NULL,    -- ISSUING / ISSUED / FAILED
    invoice_code   VARCHAR(32),           -- 发票代码
    invoice_serial VARCHAR(32),           -- 发票号码
    pdf_url     VARCHAR(512),             -- 电子发票 PDF 地址
    provider    VARCHAR(16),              -- MOCK / AISINO / BAIWANG ...
    created_at  DATETIME    NOT NULL,
    issued_at   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_taxinv_invoice ON tax_invoice(invoice_id);
CREATE INDEX idx_taxinv_status ON tax_invoice(status);
