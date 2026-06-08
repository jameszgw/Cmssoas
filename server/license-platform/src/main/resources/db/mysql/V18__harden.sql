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
