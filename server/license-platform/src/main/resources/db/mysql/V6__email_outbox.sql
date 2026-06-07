-- [MySQL 方言] 由 db/migration/V6__email_outbox.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 邮件事务发件箱：开通邮件先入库（与租户创建同事务），再由后台异步投递+重试
CREATE TABLE email_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT,
    to_addr         VARCHAR(128) NOT NULL,
    subject         VARCHAR(256) NOT NULL,
    body_html       TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,   -- PENDING / SENT / FAILED
    attempts        INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 5,
    next_attempt_at DATETIME    NOT NULL,
    last_error      VARCHAR(512),
    created_at      DATETIME    NOT NULL,
    sent_at         DATETIME
);
CREATE INDEX idx_outbox_due ON email_outbox(status, next_attempt_at);
