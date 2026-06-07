-- [MySQL 方言] 由 db/migration/V2__add_mfa_secret.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0、建表 utf8mb4。
-- 字符集在建表处显式 utf8mb4,兼容 MySQL 5.7/8.0(不依赖库级默认字符集)。
-- 为超管增加 MFA(TOTP) 密钥列
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(64);
