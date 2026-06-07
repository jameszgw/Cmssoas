-- [MySQL 方言] 由 db/migration/V2__add_mfa_secret.sql 机械转换:IDENTITY→AUTO_INCREMENT、TIMESTAMP→DATETIME、布尔默认 1/0。
-- 字符集请在库级设为 utf8mb4(见 application-mysql.yml 的连接串/CREATE DATABASE)。
-- 为超管增加 MFA(TOTP) 密钥列
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(64);
