-- ============================================================
-- CODEMAN 全量表结构 + 系统初始化数据(MySQL 5.7 / 8.0)
-- 自动生成,请勿手改;由 deploy/sql/generate-schema.sh 合并 Flyway 迁移而来。
-- 生成时间: 2026-06-11 17:32:54
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

-- ---------- V18__harden.sql ----------
-- [MySQL 方言] 由 db/migration/V18__harden.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 在线代码加固(上传 jar → 异步加固 → 下载)。与"构建/打包"加固并存,按租户设默认模式 + 任务可覆盖。
CREATE TABLE harden_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code  VARCHAR(32)  NOT NULL,
    mode         VARCHAR(16)  NOT NULL DEFAULT 'BUILD',   -- BUILD(仅构建自建) / ONLINE(仅平台在线) / BOTH(并存)
    obfuscate    BOOLEAN      NOT NULL DEFAULT 1,       -- 默认勾选:ProGuard 混淆
    encrypt_bind BOOLEAN      NOT NULL DEFAULT 0,      -- 默认勾选:类加密并与 License 绑定
    fatjar_encrypt BOOLEAN    NOT NULL DEFAULT 0,      -- 默认勾选:口令式整包/类加密
    updated_at   DATETIME    NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE UNIQUE INDEX uk_harden_config_tenant ON harden_config(tenant_code);

CREATE TABLE harden_job (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_no        VARCHAR(40)  NOT NULL,
    tenant_code   VARCHAR(32),
    source_name   VARCHAR(200) NOT NULL,   -- 上传原始文件名
    source_size   BIGINT       NOT NULL,
    techniques    VARCHAR(128) NOT NULL,   -- 逗号分隔:OBFUSCATE,ENCRYPT_BIND,FATJAR_ENCRYPT(按序执行)
    bind_license  VARCHAR(40),             -- ENCRYPT_BIND 绑定的 License 编号
    status        VARCHAR(16)  NOT NULL,   -- QUEUED / RUNNING / DONE / FAILED
    message       VARCHAR(1000),           -- 处理摘要或错误
    out_size      BIGINT,
    created_at    DATETIME    NOT NULL,
    finished_at   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE INDEX idx_harden_job_tenant ON harden_job(tenant_code);
CREATE INDEX idx_harden_job_status ON harden_job(status);

INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('harden','代码加固',NULL,'MENU',13),
 ('harden:view','查看加固任务','harden','ACTION',1),
 ('harden:run','提交加固任务','harden','ACTION',2),
 ('harden:config','配置加固模式','harden','ACTION',3);

-- ---------- V19__cmprint_catalog.sql ----------
-- [MySQL 方言] 由 db/migration/V19__cmprint_catalog.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 本迁移无建表,仅 ALTER + 种子数据;TRUE 改 1 以兼容 MySQL 5.7。
-- CmPrint(打印模板设计器)商业授权集成:
-- 1) plan 表补「产品/版本档位」两列——订阅自动签发时按套餐落 productCode 与 edition(此前写死 CODEMAN/套餐码);
-- 2) 产品目录登记 CMPRINT 产品(模块=能力域,功能点=CmPrint capabilities 能力键,与前端 resolveEdition 同名);
-- 3) 三档套餐 社区/专业/企业,features JSON 与 CmPrint EDITIONS 预设逐键一致(License claims.features 原样
--    喂给 CmPrint 的 resolveEdition(edition, overrides),显式键优先于前端预设,防两侧漂移);
-- 4) CmPrint 授权菜单与操作权限点(签发/审计查询)。

ALTER TABLE plan ADD COLUMN product_code VARCHAR(32) NOT NULL DEFAULT 'CODEMAN';
ALTER TABLE plan ADD COLUMN edition VARCHAR(32);
-- 存量 CODEMAN 套餐:套餐码即版本档位(BASIC/PROFESSIONAL/ENTERPRISE/FLAGSHIP),与原签发行为一致
UPDATE plan SET edition = code WHERE product_code = 'CODEMAN';

-- ===== CMPRINT 产品目录 =====
INSERT INTO product(code,name) VALUES ('CMPRINT','CmPrint 打印模板设计器');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CMPRINT','DESIGN','设计器',1),
 ('CMPRINT','DATA','数据',2),
 ('CMPRINT','OUTPUT','预览打印',3),
 ('CMPRINT','EXPORT','导出',4),
 ('CMPRINT','TEMPLATE','模板',5);

-- 功能点 code = CmPrint 能力键(src/core/capabilities.js CAPABILITY_KEYS),License features 与其同名
INSERT INTO feature(module_code,code,name,sort) VALUES
 ('DESIGN','theme','设计器换肤',1),
 ('DATA','dataSource','数据源编辑器',2),
 ('OUTPUT','preview','测量分页预览',3),
 ('OUTPUT','print','浏览器打印',4),
 ('OUTPUT','directPrint','直连静默打印',5),
 ('OUTPUT','watermark','水印',6),
 ('OUTPUT','overprint','套打底图',7),
 ('OUTPUT','calibration','打印对位校准',8),
 ('EXPORT','exportJson','模板 JSON 导出',9),
 ('EXPORT','exportPdf','PDF 导出(图片型)',10),
 ('EXPORT','exportPdfText','PDF 导出(文本型)',11),
 ('EXPORT','sharePdf','PDF 系统分享',12),
 ('EXPORT','exportWord','Word 导出',13),
 ('EXPORT','exportExcel','Excel/CSV 导出',14),
 ('EXPORT','exportImage','图片导出',15),
 ('TEMPLATE','subTemplate','子模板',16),
 ('TEMPLATE','templateGallery','云端模板库',17);

INSERT INTO app_version(product_code,version,sort) VALUES ('CMPRINT','v0.5',1);

INSERT INTO feature_version(feature_code,version,available) VALUES
 ('theme','v0.5',1),('dataSource','v0.5',1),('preview','v0.5',1),('print','v0.5',1),
 ('directPrint','v0.5',1),('watermark','v0.5',1),('overprint','v0.5',1),('calibration','v0.5',1),
 ('exportJson','v0.5',1),('exportPdf','v0.5',1),('exportPdfText','v0.5',1),('sharePdf','v0.5',1),
 ('exportWord','v0.5',1),('exportExcel','v0.5',1),('exportImage','v0.5',1),
 ('subTemplate','v0.5',1),('templateGallery','v0.5',1);

-- 三档套餐:features = CmPrint EDITIONS 预设(社区关 13 键 / 专业关 2 键 / 企业全开)
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort,product_code,edition) VALUES
 ('CMPRINT_COMMUNITY','CmPrint 社区版','plan.cmp.com',0,'>=0.5.0 <1.0.0',1,'DESIGN,DATA,OUTPUT,TEMPLATE',
   '{"exportPdf":false,"exportPdfText":false,"sharePdf":false,"exportWord":false,"exportExcel":false,"exportImage":false,"directPrint":false,"overprint":false,"calibration":false,"watermark":false,"subTemplate":false,"templateGallery":false,"theme":false}',
   'ACTIVE',11,'CMPRINT','COMMUNITY'),
 ('CMPRINT_PRO','CmPrint 专业版','plan.cmp.pro',12800,'>=0.5.0 <1.0.0',5,'DESIGN,DATA,OUTPUT,EXPORT,TEMPLATE',
   '{"directPrint":false,"templateGallery":false}','ACTIVE',12,'CMPRINT','PROFESSIONAL'),
 ('CMPRINT_ENT','CmPrint 企业版','plan.cmp.ent',39800,'>=0.5.0 <1.0.0',20,'DESIGN,DATA,OUTPUT,EXPORT,TEMPLATE',
   '{}','ACTIVE',13,'CMPRINT','ENTERPRISE');

-- ===== CmPrint 授权权限点 =====
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('cmprint','CmPrint 授权',NULL,'MENU',14),
 ('cmprint:view','查看 CmPrint 授权','cmprint','ACTION',1),
 ('cmprint:issue','签发 CmPrint 授权','cmprint','ACTION',2),
 ('cmprint:audit','CmPrint 审计查询','cmprint','ACTION',3);

-- ---------- V20__cmrule_catalog.sql ----------
-- [MySQL 方言] 由 db/migration/V20__cmrule_catalog.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 本迁移无建表,仅种子数据;TRUE 改 1 以兼容 MySQL 5.7。
-- CmRuleEngine(规则引擎)商业授权集成:
-- 1) 产品目录登记 CMRULE 产品(模块=能力域,功能点=CmRuleEngine capabilities 能力键,
--    与 rule-engine-server/src/capabilities.js 的 CAPABILITY_KEYS 逐键同名);
-- 2) 四档套餐 社区/专业/企业/旗舰,features JSON 与 CmRuleEngine PRESETS 预设逐键一致
--    (License claims.features 原样喂给客户端 resolve(edition, overrides),显式键优先于内置预设,防两侧漂移);
-- 3) CmRuleEngine 授权菜单与操作权限点(签发/审计查询)。
-- 注:plan 的 product_code/edition 两列已由 V19 引入,本迁移直接使用。

-- ===== CMRULE 产品目录 =====
INSERT INTO product(code,name) VALUES ('CMRULE','CmRuleEngine 规则引擎');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CMRULE','CORE','基础设计',1),
 ('CMRULE','LOWCODE','低代码零代码',2),
 ('CMRULE','DECISION','决策建模',3),
 ('CMRULE','FLOW','规则流',4),
 ('CMRULE','VERSION','版本管理',5),
 ('CMRULE','ORCH','服务编排',6),
 ('CMRULE','AUDITLOG','审计',7),
 ('CMRULE','PLATFORM','平台增强',8),
 ('CMRULE','AI','智能',9);

-- 功能点 code = CmRuleEngine 能力键(rule-engine-server/src/capabilities.js CAPABILITY_KEYS),License features 与其同名
INSERT INTO feature(module_code,code,name,sort) VALUES
 ('CORE','ruleChainCore','基础规则链设计与运行',1),
 ('LOWCODE','zeroCodeWizard','零代码规则向导',2),
 ('LOWCODE','lintCheck','保存前体检',3),
 ('LOWCODE','nodeSearch','节点搜索',4),
 ('LOWCODE','templateLibrary','模板库',5),
 ('DECISION','decisionTable','决策表节点',6),
 ('FLOW','ruleFlow','规则流(子链串联)',7),
 ('FLOW','edgeChains','EDGE 规则链',8),
 ('VERSION','versionHistory','版本管理(快照/回滚)',9),
 ('VERSION','versionDiff','影响分析(版本对比)',10),
 ('ORCH','orchestrationBasic','服务编排基本节点(REST 外呼)',11),
 ('ORCH','orchestrationAdvanced','高级服务编排(消息/DB/补偿)',12),
 ('AUDITLOG','auditQuery','审计查询',13),
 ('AUDITLOG','auditExport','审计导出 CSV',14),
 ('PLATFORM','dbDialects','国产数据库适配',15),
 ('PLATFORM','haCluster','高可用集群',16),
 ('PLATFORM','distributedTx','分布式事务',17),
 ('PLATFORM','xinChuang','信创认证',18),
 ('AI','aiRuleGen','AI 生成规则链',19);

INSERT INTO app_version(product_code,version,sort) VALUES ('CMRULE','v1.0',1);

INSERT INTO feature_version(feature_code,version,available) VALUES
 ('ruleChainCore','v1.0',1),('zeroCodeWizard','v1.0',1),('lintCheck','v1.0',1),('nodeSearch','v1.0',1),
 ('templateLibrary','v1.0',1),('decisionTable','v1.0',1),('ruleFlow','v1.0',1),('edgeChains','v1.0',1),
 ('versionHistory','v1.0',1),('versionDiff','v1.0',1),('orchestrationBasic','v1.0',1),
 ('orchestrationAdvanced','v1.0',1),('auditQuery','v1.0',1),('auditExport','v1.0',1),
 ('dbDialects','v1.0',1),('haCluster','v1.0',1),('distributedTx','v1.0',1),('xinChuang','v1.0',1),
 ('aiRuleGen','v1.0',1);

-- 四档套餐:features = CmRuleEngine PRESETS 预设(社区关 15 键 / 专业关 8 键 / 企业关 3 键 / 旗舰全开)
INSERT INTO plan(code,name,plan_key,price,version_range,seats,modules,features,status,sort,product_code,edition) VALUES
 ('CMRULE_COMMUNITY','CmRuleEngine 社区版','plan.cmr.com',0,'>=1.0.0 <2.0.0',1,'CORE,LOWCODE',
   '{"templateLibrary":false,"decisionTable":false,"ruleFlow":false,"edgeChains":false,"versionHistory":false,"orchestrationBasic":false,"auditQuery":false,"versionDiff":false,"auditExport":false,"orchestrationAdvanced":false,"dbDialects":false,"haCluster":false,"aiRuleGen":false,"distributedTx":false,"xinChuang":false}',
   'ACTIVE',14,'CMRULE','COMMUNITY'),
 ('CMRULE_PRO','CmRuleEngine 专业版','plan.cmr.pro',16800,'>=1.0.0 <2.0.0',5,'CORE,LOWCODE,DECISION,FLOW,VERSION,ORCH,AUDITLOG',
   '{"versionDiff":false,"auditExport":false,"orchestrationAdvanced":false,"dbDialects":false,"haCluster":false,"aiRuleGen":false,"distributedTx":false,"xinChuang":false}',
   'ACTIVE',15,'CMRULE','PROFESSIONAL'),
 ('CMRULE_ENT','CmRuleEngine 企业版','plan.cmr.ent',49800,'>=1.0.0 <2.0.0',20,'CORE,LOWCODE,DECISION,FLOW,VERSION,ORCH,AUDITLOG,PLATFORM',
   '{"aiRuleGen":false,"distributedTx":false,"xinChuang":false}','ACTIVE',16,'CMRULE','ENTERPRISE'),
 ('CMRULE_ULT','CmRuleEngine 旗舰版','plan.cmr.ult',158000,'>=1.0.0 <2.0.0',999,'CORE,LOWCODE,DECISION,FLOW,VERSION,ORCH,AUDITLOG,PLATFORM,AI',
   '{}','ACTIVE',17,'CMRULE','ULTIMATE');

-- ===== CmRuleEngine 授权权限点 =====
INSERT INTO permission(code,name,parent_code,type,sort) VALUES
 ('cmrule','CmRuleEngine 授权',NULL,'MENU',15),
 ('cmrule:view','查看 CmRuleEngine 授权','cmrule','ACTION',1),
 ('cmrule:issue','签发 CmRuleEngine 授权','cmrule','ACTION',2),
 ('cmrule:audit','CmRuleEngine 审计查询','cmrule','ACTION',3);

