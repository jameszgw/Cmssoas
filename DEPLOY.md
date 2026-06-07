# 部署与生产化(入口)

> **完整部署说明已迁移到 [`deploy/README.md`](deploy/README.md)**(环境要求、Docker/裸机、数据库初始化两法、MySQL、智能客服大模型、`.env` 等)。
> 本文仅保留开发/测试视角的补充与生产清单。

## 快速入口
- 部署总说明:[`deploy/README.md`](deploy/README.md)
- 数据库脚本(表结构/系统数据/测试数据):[`deploy/sql/`](deploy/sql/README.md)
- 操作说明 / 帮助文档:[`deploy/docs/操作说明.md`](deploy/docs/操作说明.md) · [`deploy/docs/帮助文档.md`](deploy/docs/帮助文档.md) · [`deploy/docs/README.md`](deploy/docs/README.md)(文档索引)
- 一键起:`cp .env.example .env && docker compose up -d --build`(前端 http://localhost,后端 :8080,账号 admin/8888)

## 数据库支持(速查)
| 数据库 | 状态 | 说明 |
|---|---|---|
| **H2** | ✅ 开发默认 | 内存库,开箱即跑(PostgreSQL 兼容模式) |
| **PostgreSQL** | ✅ 生产推荐 | `profile=prod`(`application-prod.yml`) |
| **MySQL 5.7 / 8.0** | ✅ 支持 | `profile=mysql` + `db/mysql` 方言脚本;CI 在 5.7/8.0 真机矩阵验证 |

> 运行环境:JDK **17+(推荐 21)**;不支持 Java 8(Spring Boot 3.5)。初始化步骤见 `deploy/README.md` 第 3 节。

## 生产化要点
| 能力 | 说明 |
|------|------|
| 数据库 | Flyway 完全管理 schema(`ddl-auto=none`,跨 H2/PG/MySQL);各方言迁移由 CI 真机验证。 |
| Redis | `APP_REDIS_ENABLED=true` 时心跳 nonce 防重放走 Redis(`SET NX EX`),支持后端多实例水平扩展。 |
| 邮件发件箱(outbox) | 开通邮件与租户创建**同事务**写 `email_outbox`,`OutboxDispatcher` 异步投递 + 指数退避重试。 |
| 鉴权 | 全接口 JWT + RBAC(`@RequirePerm`);超管自动全量授权;详见角色权限模块。 |
| CI | `.github/workflows/ci.yml`:后端 / SDK+示例 / 前端 / 签名矩阵 / E2E / **MySQL 5.7·8.0 真机迁移**。 |

## 本地开发(无需 Docker)
默认 H2 内存库 + 邮件落盘,开箱即跑:
```bash
cd server/license-platform && mvn spring-boot:run     # :8080
cd web/console && npm install && npm run dev          # :5173 (代理到 :8080)
```

## 测试与覆盖率
```bash
cd server/license-platform && mvn test    # 全栈集成测试 + 单元测试;JaCoCo: target/site/jacoco/index.html
cd sdk/license-sdk && mvn test            # SemVer / 验签 / 防篡改;JaCoCo 同上
```
- 后端含登录/JWT/权限拦截、须知授权、客服降级、合同签署→出账、支付收款、电子发票、客户360、自助门户、License 吊销 CRL 验签与到期停用等集成测试。
- MySQL 5.7/8.0 方言迁移由 CI 在真实实例上验证(`MysqlRealMigrationTest`);本地 `MysqlMigrationTest` 走 H2 MySQL 兼容模式烟测。
- 审计:License/订阅/角色/用户/支付/开票/合同等关键操作写入 `audit_log`,在「审计追溯」可查。

## 生产清单(建议继续)
- 私钥托管 KMS/HSM(当前演示落盘 `var/keys`);`JWT_SECRET`/数据库口令经 `.env`/Nacos/KMS 注入,勿入库。
- 后端多实例 + 负载均衡;签发服务与在线校验服务可分离部署。
- 数据库主从 + 备份(PITR);Redis 哨兵/集群。
- mTLS(SDK 通道)、WAF/网关限流、可观测(Prometheus/Grafana/ELK)。
- 支付/电子签/电子发票按需切换为真实渠道(`app.pay.provider` / `app.einvoice.provider`)。
