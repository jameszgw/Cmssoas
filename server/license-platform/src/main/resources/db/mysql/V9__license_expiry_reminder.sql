-- [MySQL 方言] 由 db/migration/V9__license_expiry_reminder.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 到期提醒去重标记：避免同一 License 重复发提醒邮件
ALTER TABLE license ADD COLUMN expiry_reminded BOOLEAN NOT NULL DEFAULT 0;
