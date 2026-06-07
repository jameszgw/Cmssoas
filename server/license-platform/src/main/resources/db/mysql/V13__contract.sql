-- [MySQL 方言] 由 db/migration/V13__contract.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 合同签约(电子签：自建签章留痕，哈希+时间戳存证)
CREATE TABLE contract_template (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(160) NOT NULL,
    content_html TEXT         NOT NULL,   -- 含占位符 {{customer}} {{amount}} {{plan}} {{tenant}} {{date}}
    variables    VARCHAR(256),
    created_at   DATETIME    NOT NULL
);

CREATE TABLE contract (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_no     VARCHAR(40),
    tenant_code     VARCHAR(32),
    customer        VARCHAR(128) NOT NULL,
    subscription_id BIGINT,
    plan_code       VARCHAR(32),
    template_id     BIGINT,
    title           VARCHAR(160) NOT NULL,
    content_html    TEXT         NOT NULL,   -- 发起签署时的内容快照(不可改)
    amount          INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL,   -- DRAFT/SENT/SIGNING/SIGNED/VOID
    content_hash    VARCHAR(80),             -- 内容 SHA-256 存证
    created_at      DATETIME    NOT NULL,
    sent_at         DATETIME,
    signed_at       DATETIME
);
CREATE INDEX idx_contract_tenant ON contract(tenant_code);
CREATE INDEX idx_contract_status ON contract(status);

CREATE TABLE contract_party (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT      NOT NULL,
    name        VARCHAR(128) NOT NULL,
    party_role  VARCHAR(32),             -- 甲方/乙方/...
    email       VARCHAR(128),
    phone       VARCHAR(32),
    sign_status VARCHAR(16) NOT NULL,    -- PENDING/SIGNED
    sign_hash   VARCHAR(80),             -- 签署存证哈希
    signed_at   DATETIME
);
CREATE INDEX idx_party_contract ON contract_party(contract_id);

-- 权限点
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('contract','合同签约',NULL,'MENU',12),
 ('contract:view','查看合同','contract','ACTION',1),
 ('contract:edit','创建/发起签署','contract','ACTION',2),
 ('contract:sign','代签/确认签署','contract','ACTION',3);
