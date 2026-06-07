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
INSERT INTO product(code,name) VALUES ('CMSSOAS','软件授权管理平台');

INSERT INTO module(product_code,code,name,sort) VALUES
 ('CMSSOAS','RISK','风险管理',1),
 ('CMSSOAS','REPORT','报表中心',2),
 ('CMSSOAS','AUDIT','审计',3),
 ('CMSSOAS','BI','智能分析',4);

INSERT INTO feature(module_code,code,name,sort) VALUES
 ('RISK','RISK.RULES','规则引擎',1),
 ('RISK','RISK.REALTIME','实时风控',2),
 ('REPORT','REPORT.VIEW','报表查看',3),
 ('REPORT','REPORT.EXPORT','报表导出',4),
 ('AUDIT','AUDIT.TRAIL','操作审计',5),
 ('BI','BI.DASH','分析大盘',6),
 ('BI','BI.ML','智能预测',7);

INSERT INTO app_version(product_code,version,sort) VALUES
 ('CMSSOAS','v2.2',1),('CMSSOAS','v2.3',2),('CMSSOAS','v2.4',3);

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
