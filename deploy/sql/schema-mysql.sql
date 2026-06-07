-- ============================================================
-- CODEMAN 全量表结构 + 系统初始化数据(MySQL 5.7 / 8.0)
-- 自动生成,请勿手改;由 deploy/sql/generate-schema.sh 合并 Flyway 迁移而来。
-- 生成时间: 2026-06-07 14:53:22
-- 含:全部业务表 DDL + 系统数据(权限/角色/套餐等种子)。
-- 注:初始超管账号 admin/8888 由应用首启自动创建(或见 init-admin.sql)。
-- ============================================================

-- ---------- V1__init.sql ----------
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

-- ---------- V2__add_mfa_secret.sql ----------
-- [MySQL 方言] 由 db/migration/V2__add_mfa_secret.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 为超管增加 MFA(TOTP) 密钥列
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(64);

-- ---------- V3__license.sql ----------
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

-- ---------- V4__license_instance.sql ----------
-- [MySQL 方言] 由 db/migration/V4__license_instance.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 在线授权：License 激活实例（浮动席位）
CREATE TABLE license_instance (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_id     VARCHAR(40)  NOT NULL,
    instance_id    VARCHAR(64)  NOT NULL,
    machine_code   VARCHAR(80),
    ip             VARCHAR(64),
    status         VARCHAR(16)  NOT NULL,  -- ACTIVE / RELEASED / EXPIRED
    activated_at   DATETIME    NOT NULL,
    last_heartbeat DATETIME    NOT NULL,
    CONSTRAINT uk_lic_inst UNIQUE (license_id, instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_inst_lic ON license_instance(license_id);
CREATE INDEX idx_inst_status ON license_instance(status);

-- ---------- V5__catalog_plan.sql ----------
-- [MySQL 方言] 由 db/migration/V5__catalog_plan.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 产品目录 / 套餐 / 订阅

CREATE TABLE product (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE module (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(32) NOT NULL,
    code         VARCHAR(32) NOT NULL,
    name         VARCHAR(64) NOT NULL,
    sort         INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE feature (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_code VARCHAR(32) NOT NULL,
    code        VARCHAR(48) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    sort        INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE app_version (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(32) NOT NULL,
    version      VARCHAR(16) NOT NULL,
    sort         INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE feature_version (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    feature_code VARCHAR(48) NOT NULL,
    version      VARCHAR(16) NOT NULL,
    available    BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE plan (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL UNIQUE,
    name          VARCHAR(64)  NOT NULL,
    plan_key      VARCHAR(32)  NOT NULL,
    price         INT          NOT NULL,
    version_range VARCHAR(64)  NOT NULL,
    seats         INT          NOT NULL,
    modules       VARCHAR(512) NOT NULL,
    features      VARCHAR(2000) NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    sort          INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE subscription (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(32)  NOT NULL,
    customer    VARCHAR(128) NOT NULL,
    plan_code   VARCHAR(32)  NOT NULL,
    qty         INT          NOT NULL,
    start_at    DATE         NOT NULL,
    end_at      DATE         NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    license_id  VARCHAR(40),
    created_at  DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ===== 种子数据 =====
INSERT INTO product(code,name) VALUES ('CODEMAN','软件授权管理平台');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CODEMAN','RISK','风险管理',1),
 ('CODEMAN','REPORT','报表中心',2),
 ('CODEMAN','AUDIT','审计',3),
 ('CODEMAN','BI','智能分析',4);

INSERT INTO feature(module_code,code,name,sort) VALUES
 ('RISK','RISK.RULES','规则引擎',1),
 ('RISK','RISK.REALTIME','实时风控',2),
 ('REPORT','REPORT.VIEW','报表查看',3),
 ('REPORT','REPORT.EXPORT','报表导出',4),
 ('AUDIT','AUDIT.TRAIL','操作审计',5),
 ('BI','BI.DASH','分析大盘',6),
 ('BI','BI.ML','智能预测',7);

INSERT INTO app_version(product_code,version,sort) VALUES
 ('CODEMAN','v2.2',1),('CODEMAN','v2.3',2),('CODEMAN','v2.4',3);

INSERT INTO feature_version(feature_code,version,available) VALUES
 ('RISK.RULES','v2.2',TRUE),('RISK.RULES','v2.3',TRUE),('RISK.RULES','v2.4',TRUE),
 ('RISK.REALTIME','v2.2',FALSE),('RISK.REALTIME','v2.3',TRUE),('RISK.REALTIME','v2.4',TRUE),
 ('REPORT.VIEW','v2.2',TRUE),('REPORT.VIEW','v2.3',TRUE),('REPORT.VIEW','v2.4',TRUE),
 ('REPORT.EXPORT','v2.2',FALSE),('REPORT.EXPORT','v2.3',TRUE),('REPORT.EXPORT','v2.4',TRUE),
 ('AUDIT.TRAIL','v2.2',TRUE),('AUDIT.TRAIL','v2.3',TRUE),('AUDIT.TRAIL','v2.4',TRUE),
 ('BI.DASH','v2.2',FALSE),('BI.DASH','v2.3',FALSE),('BI.DASH','v2.4',TRUE),
 ('BI.ML','v2.2',FALSE),('BI.ML','v2.3',FALSE),('BI.ML','v2.4',TRUE);

INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort) VALUES
 ('BASIC','基础版','plan.basic',19800,'>=2.2.0 <3.0.0',2,'RISK,REPORT',
   '{"REPORT.VIEW":true,"MAX_USERS":50}','ACTIVE',1),
 ('PROFESSIONAL','专业版','plan.pro',49800,'>=2.3.0 <3.0.0',5,'RISK,REPORT,AUDIT',
   '{"REPORT.EXPORT":true,"AUDIT.TRAIL":true,"MAX_USERS":200}','ACTIVE',2),
 ('ENTERPRISE','企业版','plan.ent',99800,'>=2.4.0 <3.0.0',20,'RISK,REPORT,AUDIT,BI',
   '{"REPORT.EXPORT":true,"RISK.REALTIME":true,"BI.DASH":true,"MAX_USERS":2000}','ACTIVE',3),
 ('FLAGSHIP','旗舰版','plan.flag',198000,'>=2.4.0 <3.0.0',50,'RISK,REPORT,AUDIT,BI,DATA',
   '{"REPORT.EXPORT":true,"RISK.REALTIME":true,"BI.DASH":true,"BI.ML":true,"MAX_USERS":99999}','ACTIVE',4);

-- ---------- V6__email_outbox.sql ----------
-- [MySQL 方言] 由 db/migration/V6__email_outbox.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_outbox_due ON email_outbox(status, next_attempt_at);

-- ---------- V7__rbac.sql ----------
-- [MySQL 方言] 由 db/migration/V7__rbac.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- RBAC：权限树 / 角色 / 角色-权限(多态 mode) / 运营用户

CREATE TABLE permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(48) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    parent_code VARCHAR(48),
    type        VARCHAR(16) NOT NULL,   -- MENU / ACTION
    sort        INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE ops_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(32) NOT NULL UNIQUE,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(128)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE ops_role_permission (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id   BIGINT NOT NULL,
    perm_code VARCHAR(48) NOT NULL,
    mode      VARCHAR(16) NOT NULL,   -- NONE / VIEW / EDIT / FULL
    CONSTRAINT uk_role_perm UNIQUE (role_id, perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE ops_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    role_id         BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL,
    must_change_pwd BOOLEAN NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 权限树种子
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('overview','运营总览',NULL,'MENU',1),
 ('tenant','租户管理',NULL,'MENU',2),
 ('tenant:view','查看租户','tenant','ACTION',1),
 ('tenant:onboard','开通租户','tenant','ACTION',2),
 ('tenant:renew','续期租户','tenant','ACTION',3),
 ('license','License 授权',NULL,'MENU',3),
 ('license:view','查看 License','license','ACTION',1),
 ('license:issue','签发 License','license','ACTION',2),
 ('license:renew','续期 License','license','ACTION',3),
 ('license:revoke','吊销 License','license','ACTION',4),
 ('license:download','下载 .lic','license','ACTION',5),
 ('online','在线监控',NULL,'MENU',4),
 ('online:view','查看在线实例','online','ACTION',1),
 ('catalog','产品与版本',NULL,'MENU',5),
 ('catalog:view','查看产品矩阵','catalog','ACTION',1),
 ('plan','套餐订阅',NULL,'MENU',6),
 ('plan:view','查看套餐','plan','ACTION',1),
 ('plan:subscribe','创建订阅','plan','ACTION',2),
 ('audit','审计追溯',NULL,'MENU',7),
 ('audit:view','查看审计','audit','ACTION',1),
 ('system','系统管理',NULL,'MENU',8),
 ('role:view','查看角色权限','system','ACTION',1),
 ('role:edit','编辑角色权限','system','ACTION',2),
 ('user:view','查看用户','system','ACTION',3);

-- 角色种子（SUPER_ADMIN 的权限映射由 DataInitializer 填充为 FULL）
INSERT INTO ops_role(code,name,description) VALUES
 ('SUPER_ADMIN','超级管理员','拥有全部权限'),
 ('OPERATOR','运营专员','日常运营操作'),
 ('VIEWER','只读访客','仅查看');

-- ---------- V8__user_edit_perm.sql ----------
-- [MySQL 方言] 由 db/migration/V8__user_edit_perm.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 新增「编辑用户」权限点（用户管理）
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('user:edit','编辑用户','system','ACTION',4);

-- ---------- V9__license_expiry_reminder.sql ----------
-- [MySQL 方言] 由 db/migration/V9__license_expiry_reminder.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 到期提醒去重标记：避免同一 License 重复发提醒邮件
ALTER TABLE license ADD COLUMN expiry_reminded BOOLEAN NOT NULL DEFAULT 0;

-- ---------- V10__invoice.sql ----------
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

-- ---------- V11__notice_consent.sql ----------
-- [MySQL 方言] 由 db/migration/V11__notice_consent.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 在线用户须知 / 用户授权(同意管理)
CREATE TABLE notice (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    type          VARCHAR(16)  NOT NULL,   -- TERMS / PRIVACY / NOTICE / ANNOUNCEMENT
    title         VARCHAR(160) NOT NULL,
    content_html  TEXT         NOT NULL,
    version       INT          NOT NULL DEFAULT 1,
    status        VARCHAR(16)  NOT NULL,   -- DRAFT / PUBLISHED / ARCHIVED
    force_ack     BOOLEAN      NOT NULL DEFAULT 0,
    effective_at  DATETIME,
    created_at    DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_notice_type_status ON notice(type, status);

CREATE TABLE user_consent (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code  VARCHAR(32),
    subject      VARCHAR(128) NOT NULL,    -- 同意主体(运营账号/邮箱/手机号)
    notice_id    BIGINT       NOT NULL,
    notice_type  VARCHAR(16)  NOT NULL,
    version      INT          NOT NULL,
    action       VARCHAR(16)  NOT NULL,    -- GRANTED / REVOKED
    channel      VARCHAR(16)  NOT NULL,    -- WEB / EMAIL / API
    ip           VARCHAR(64),
    user_agent   VARCHAR(256),
    created_at   DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_consent_subject ON user_consent(subject);
CREATE INDEX idx_consent_notice ON user_consent(notice_id);

-- 权限点
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('notice','须知与授权',NULL,'MENU',10),
 ('notice:view','查看须知/授权','notice','ACTION',1),
 ('notice:edit','编辑/发布须知','notice','ACTION',2),
 ('consent:view','查看授权记录','notice','ACTION',3);

-- ---------- V12__customer_service.sql ----------
-- [MySQL 方言] 由 db/migration/V12__customer_service.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 智能客服：会话 + 消息（通用 provider-agnostic，不绑定厂商）
CREATE TABLE cs_conversation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code  VARCHAR(32),
    user_ref     VARCHAR(128) NOT NULL,   -- 发起人(运营账号/访客标识)
    title        VARCHAR(160),
    status       VARCHAR(16)  NOT NULL,   -- OPEN / ESCALATED / CLOSED
    created_at   DATETIME    NOT NULL,
    updated_at   DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_cs_conv_user ON cs_conversation(user_ref);
CREATE INDEX idx_cs_conv_status ON cs_conversation(status);

CREATE TABLE cs_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    role            VARCHAR(16) NOT NULL,   -- user / assistant / system
    content         TEXT        NOT NULL,
    created_at      DATETIME   NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_cs_msg_conv ON cs_message(conversation_id);

-- 权限点
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('cs','智能客服',NULL,'MENU',11),
 ('cs:use','使用客服对话','cs','ACTION',1),
 ('cs:view','查看客服会话(审计)','cs','ACTION',2);

-- ---------- V13__contract.sql ----------
-- [MySQL 方言] 由 db/migration/V13__contract.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 合同签约(电子签：自建签章留痕，哈希+时间戳存证)
CREATE TABLE contract_template (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(160) NOT NULL,
    content_html TEXT         NOT NULL,   -- 含占位符 {{customer}} {{amount}} {{plan}} {{tenant}} {{date}}
    variables    VARCHAR(256),
    created_at   DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_party_contract ON contract_party(contract_id);

-- 权限点
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('contract','合同签约',NULL,'MENU',12),
 ('contract:view','查看合同','contract','ACTION',1),
 ('contract:edit','创建/发起签署','contract','ACTION',2),
 ('contract:sign','代签/确认签署','contract','ACTION',3);

-- ---------- V14__payment.sql ----------
-- [MySQL 方言] 由 db/migration/V14__payment.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 在线支付/收款(通用 PaymentProvider 抽象,默认沙箱;预留微信支付/支付宝/Stripe)
CREATE TABLE payment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_no      VARCHAR(40)  NOT NULL,   -- 本系统支付单号
    invoice_id      BIGINT       NOT NULL,
    tenant_code     VARCHAR(32),
    customer        VARCHAR(128),
    amount          INT          NOT NULL,
    currency        VARCHAR(8)   NOT NULL DEFAULT 'CNY',
    channel         VARCHAR(16)  NOT NULL,   -- MOCK / WECHATPAY / ALIPAY / STRIPE
    status          VARCHAR(16)  NOT NULL,   -- CREATED / PAID / FAILED / CLOSED
    qr_content      VARCHAR(512),            -- 扫码支付内容(二维码原文)
    pay_url         VARCHAR(512),            -- 收银台/支付跳转链接
    provider_txn_id VARCHAR(64),             -- 渠道交易号
    created_at      DATETIME    NOT NULL,
    paid_at         DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_payment_invoice ON payment(invoice_id);
CREATE INDEX idx_payment_no ON payment(payment_no);
CREATE INDEX idx_payment_status ON payment(status);

-- 复用 billing 权限:发起收款=billing:manage,查询=billing:view,异步回调公开。

-- ---------- V15__customer.sql ----------
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

-- ---------- V16__tenant_portal.sql ----------
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

-- ---------- V17__tax_invoice.sql ----------
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

