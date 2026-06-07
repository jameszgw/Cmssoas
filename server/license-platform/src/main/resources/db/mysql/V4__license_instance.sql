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
