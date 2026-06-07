-- [MySQL 方言] 由 db/migration/V3__license.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- License 签发与生命周期

CREATE TABLE license (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_id        VARCHAR(40)  NOT NULL UNIQUE,
    tenant_code       VARCHAR(32)  NOT NULL,
    customer          VARCHAR(128) NOT NULL,
    product_code      VARCHAR(32)  NOT NULL,
    edition           VARCHAR(32)  NOT NULL,
    mode              VARCHAR(16)  NOT NULL,
    modules           VARCHAR(512) NOT NULL,   -- 逗号分隔
    features          VARCHAR(2000) NOT NULL,  -- JSON
    app_version_range VARCHAR(64)  NOT NULL,
    not_before        DATE         NOT NULL,
    not_after         DATE         NOT NULL,
    concurrency       INT          NOT NULL,
    watermark         VARCHAR(64)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    current_version   INT          NOT NULL,
    claims_json       TEXT         NOT NULL,
    signature         VARCHAR(256) NOT NULL,
    lic               TEXT         NOT NULL,
    created_at        DATETIME    NOT NULL,
    updated_at        DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE license_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_id  VARCHAR(40)  NOT NULL,
    version     INT          NOT NULL,
    op_type     VARCHAR(16)  NOT NULL,   -- ISSUE / RENEW / MODIFY / REVOKE
    claims_json TEXT         NOT NULL,
    signature   VARCHAR(256) NOT NULL,
    lic         TEXT         NOT NULL,
    operator    VARCHAR(64)  NOT NULL,
    reason      VARCHAR(256),
    created_at  DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE INDEX idx_license_tenant ON license(tenant_code);
CREATE INDEX idx_lichist_lid ON license_history(license_id);
