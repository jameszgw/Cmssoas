-- 为超管增加 MFA(TOTP) 密钥列
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(64);
