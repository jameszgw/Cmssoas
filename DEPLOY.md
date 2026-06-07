# 部署与生产化

## 一键起（Docker Compose）
```bash
docker compose up -d --build
# 前端  http://localhost
# 后端  http://localhost:8080
```
组件：PostgreSQL 16 · Redis 7 · 后端(Spring Boot, profile=prod) · 前端(Nginx 托管 + 反代 /api、/sdk)。

可选环境变量（邮件真实外发）：
```bash
APP_MAIL_DELIVERY=smtp MAIL_HOST=smtp.xxx MAIL_USERNAME=.. MAIL_PASSWORD=.. \
APP_MAIL_FROM=no-reply@yourdomain.com APP_ACTIVATION_BASE_URL=https://console/activate \
docker compose up -d --build
```

## 生产化要点（本次落地）
| 能力 | 说明 |
|------|------|
| PostgreSQL | `profile=prod`（`application-prod.yml`）切 PG；Flyway 完全管理 schema（`ddl-auto=none`，跨 H2/PG 兼容，大文本用 `TEXT`）。 |
| Redis | `APP_REDIS_ENABLED=true` 时，心跳 nonce 防重放走 Redis（`SET NX EX`），支持后端多实例水平扩展；默认内存(单机)。 |
| 邮件事务发件箱(outbox) | 开通邮件先写 `email_outbox`（与租户创建**同事务**，原子），`OutboxDispatcher` 定时**异步投递 + 指数退避重试**（60s/120s/240s…，达上限置 FAILED）。邮件故障不再阻塞/回滚开通。 |
| CI | `.github/workflows/ci.yml`：后端 / SDK+示例 / 前端 三条流水线。 |

## 本地开发（无需 Docker）
默认 H2 内存库 + 邮件落盘，开箱即跑：
```bash
cd server/license-platform && mvn spring-boot:run     # :8080
cd web/console && npm install && npm run dev          # :5173 (代理到 :8080)
```

## 测试与覆盖率
```bash
cd server/license-platform && mvn test    # 含全栈集成测试(登录/JWT/权限拦截) + 单元测试；JaCoCo 报告: target/site/jacoco/index.html
cd sdk/license-sdk && mvn test            # SemVer / 验签 / 防篡改 单元测试；JaCoCo 报告同上
```
- 后端 `AuthIntegrationTest`：随机端口启动全栈(H2+Flyway+RBAC 种子)，校验 admin 登录、无 token 401、带 token 200、错误密码 401、VIEWER 越权 403。
- 后端 `TotpTest`：Base32 往返、otpauth、验证码拒绝。
- SDK `SemVerTest` / `LicenseVerifierTest`：版本区间、合法验签、篡改/错密钥/过期拒绝。
- 审计：License 签发/续期/变更/吊销、订阅、角色权限变更、用户增改 均写入 `audit_log`（带操作人），在「审计追溯」可查。

> Docker 真机联调：本仓库提供的 `docker-compose.yml` 已通过 `docker compose config` 校验；在具备 Docker 守护进程的环境执行 `docker compose up -d --build` 即可一键起 PostgreSQL/Redis/后端/前端（本沙箱无守护进程，未实际拉起容器）。

## 生产清单（建议继续）
- 私钥托管 KMS/HSM（当前演示落盘 `var/keys`）。
- 后端多实例 + 负载均衡；签发服务与在线校验服务分离部署。
- 数据库主从 + 备份(PITR)；Redis 哨兵/集群。
- mTLS（SDK 通道）、WAF/网关限流、可观测(Prometheus/Grafana/ELK)。
- 登录 + RBAC 鉴权（当前接口未鉴权，见下一步）。
