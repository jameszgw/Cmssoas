-- [MySQL 方言] 由 db/migration/V12__customer_service.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 智能客服：会话 + 消息（通用 provider-agnostic，不绑定厂商）
CREATE TABLE cs_conversation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code  VARCHAR(32),
    user_ref     VARCHAR(128) NOT NULL,   -- 发起人(运营账号/访客标识)
    title        VARCHAR(160),
    status       VARCHAR(16)  NOT NULL,   -- OPEN / ESCALATED / CLOSED
    created_at   DATETIME    NOT NULL,
    updated_at   DATETIME    NOT NULL
);
CREATE INDEX idx_cs_conv_user ON cs_conversation(user_ref);
CREATE INDEX idx_cs_conv_status ON cs_conversation(status);

CREATE TABLE cs_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(16) NOT NULL,   -- user / assistant / system
    content         TEXT        NOT NULL,
    created_at      DATETIME   NOT NULL
);
CREATE INDEX idx_cs_msg_conv ON cs_message(conversation_id);

-- 权限点
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('cs','智能客服',NULL,'MENU',11),
 ('cs:use','使用客服对话','cs','ACTION',1),
 ('cs:view','查看客服会话(审计)','cs','ACTION',2);
