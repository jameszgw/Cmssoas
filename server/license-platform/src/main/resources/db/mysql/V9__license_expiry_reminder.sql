-- [MySQL 方言] 由 db/migration/V9__license_expiry_reminder.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 到期提醒去重标记：避免同一 License 重复发提醒邮件
ALTER TABLE license ADD COLUMN expiry_reminded BOOLEAN NOT NULL DEFAULT 0;
