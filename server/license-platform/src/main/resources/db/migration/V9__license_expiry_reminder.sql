-- 到期提醒去重标记：避免同一 License 重复发提醒邮件
ALTER TABLE license ADD COLUMN expiry_reminded BOOLEAN NOT NULL DEFAULT FALSE;
