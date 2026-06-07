# 签名私钥管理：KMS / HSM 接入

License 签名私钥是信任根,绝不能明文落盘到应用服务器。本项目提供从默认「本地文件」到「外部注入 / KMS / HSM」的渐进式接入。

## 加载优先级（Ed25519KeyService）
1. **外部注入(推荐)**：环境变量 `APP_LICENSE_ED25519_PRIV`(PKCS8 DER, Base64) + `APP_LICENSE_ED25519_PUB`(X.509 DER, Base64) 存在时,直接加载,**私钥不落盘**。
2. 本地文件 `var/keys/ed25519.json`(演示态,首次自动生成)。

> 已验证：注入 env 后 `/api/licenses/public-key` 返回注入的公钥,`var/keys` 无密钥文件,日志打印「从外部(KMS/env)加载」。

## 方式 A：KMS/Vault 注入为环境变量（最简，已支持）
由 KMS/Vault sidecar 或启动脚本在进程启动前把密钥写入环境变量：
```bash
# 例：从 Vault 读取后注入
export APP_LICENSE_ED25519_PRIV=$(vault kv get -field=priv secret/cmssoas/sign)
export APP_LICENSE_ED25519_PUB=$(vault kv get -field=pub  secret/cmssoas/sign)
java -jar app.jar
```
生成密钥(一次性,在安全环境)：
```bash
openssl genpkey -algorithm ed25519 -out ed.pem
openssl pkey -in ed.pem            -outform DER | base64 -w0   # -> APP_LICENSE_ED25519_PRIV
openssl pkey -in ed.pem -pubout    -outform DER | base64 -w0   # -> APP_LICENSE_ED25519_PUB
```

## 方式 B：真 KMS「签名外移」（私钥永不出域）
私钥保存在 KMS/HSM,应用不持有私钥,改为调用 KMS 的 Sign API：
- 实现一个 `SignatureService`(本项目已是接口)的 KMS 版本,`sign(data)` 调用云 KMS（阿里云 KMS / AWS KMS）或 HSM 的非对称签名接口,公钥由 KMS 提供。
- `kid` 用 KMS 的密钥版本号,天然支持轮换(见 `docs/安全增强.md`)。

## 方式 C：HSM（PKCS#11）
- 通过 JCA PKCS#11 Provider(`SunPKCS11` + HSM 厂商 .so/.dll)接入硬件 HSM,私钥在 HSM 内运算。
- 国密场景可用支持 SM2 的国密 HSM + 对应 JCE Provider,替换 `Sm2SignatureService` 的密钥来源。

## 落地建议
- 生产用「方式 B/C」让私钥不出域;过渡期可用「方式 A」(KMS 注入 env)。
- 多 kid 并存以支持无停机轮换;`/api/licenses/public-keys`(JWKS)对外发布公钥集,SDK 按 kid 验签。
- 严控 env/密钥访问审计;CI/CD 不打印密钥;镜像/制品不含密钥。
