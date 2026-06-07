-- [MySQL 方言] 由 db/migration/V1__init.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 运营平台核心表（演示用 H2 / PostgreSQL 兼容写法）

CREATE TABLE tenant (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    plan_code   VARCHAR(32)  NOT NULL,
    plan_key    VARCHAR(32)  NOT NULL,
    version     VARCHAR(16)  NOT NULL,
    isolation   VARCHAR(64)  NOT NULL,
    mode        VARCHAR(32)  NOT NULL,
    status      VARCHAR(24)  NOT NULL,
    admin_email VARCHAR(128) NOT NULL,
    email_sent  BOOLEAN      NOT NULL DEFAULT 0,
    expire_at   DATE,
    created_at  DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    username        VARCHAR(64)  NOT NULL,
    email           VARCHAR(128) NOT NULL,
    password_hash   VARCHAR(100),
    role            VARCHAR(32)  NOT NULL,
    status          VARCHAR(24)  NOT NULL,
    must_change_pwd BOOLEAN      NOT NULL DEFAULT 1,
    mfa_bound       BOOLEAN      NOT NULL DEFAULT 0,
    created_at      DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE activation_token (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    token      VARCHAR(64)  NOT NULL UNIQUE,
    tenant_id  BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE audit_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id  BIGINT,
    actor      VARCHAR(64)  NOT NULL,
    action     VARCHAR(64)  NOT NULL,
    detail     VARCHAR(512),
    created_at DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE email_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT,
    to_addr       VARCHAR(128) NOT NULL,
    subject       VARCHAR(256) NOT NULL,
    status        VARCHAR(24)  NOT NULL,
    error         VARCHAR(512),
    rendered_path VARCHAR(256),
    created_at    DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE INDEX idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_token_tenant ON activation_token(tenant_id);
CREATE INDEX idx_audit_tenant ON audit_log(tenant_id);
