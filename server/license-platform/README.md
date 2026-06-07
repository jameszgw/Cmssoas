# CODEMAN License Platform · 后端（租户开通与开通邮件服务）

Spring Boot 3 实现：`POST /api/tenants` 触发**租户开通 Saga**（建库占位 / Flyway 初始化 / 种子数据 /
创建超管 / 生成一次性激活令牌）→ 用 **JavaMailSender + Thymeleaf** 渲染并发送**开通邮件** →
管理员凭邮件中的一次性链接**激活并设密**。

## 技术栈
Spring Boot 3.3 (Java 21) · Spring Web · Spring Data JPA · Flyway · H2（默认，可换 PostgreSQL）
· Spring Mail（JavaMailSender）· Thymeleaf（邮件模板）· Bean Validation · BCrypt

## 开箱即跑
```bash
mvn spring-boot:run        # 或 mvn -DskipTests package && java -jar target/license-platform-1.0.1.jar
# 默认 H2 内存库 + 邮件 delivery=log（渲染落盘到 ./var/mail，不外发）
```

## 接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/tenants` | 开通租户（建库/初始化/创建超管 + 发开通邮件），返回 `{code, emailSent, email}` |
| GET  | `/api/tenants` | 租户列表（前端 Tenant 视图） |
| GET  | `/api/activation/{token}` | 激活页信息（校验令牌有效性，返回 `valid` 标记） |
| POST | `/api/activation/{token}` | 提交激活并设置管理员密码（一次性、限时） |

### 示例
```bash
# 开通
curl -X POST localhost:8080/api/tenants -H 'Content-Type: application/json' \
  -d '{"name":"华东数据科技有限公司","plan":"企业版","version":"v2.4.0","email":"admin@huadong-tech.com"}'
# → {"code":"T-100483","emailSent":true,"email":"admin@huadong-tech.com"}

# 渲染的开通邮件（delivery=log 模式）
ls var/mail/*.html

# 激活（token 取自邮件中的激活链接）
curl localhost:8080/api/activation/<TOKEN>
curl -X POST localhost:8080/api/activation/<TOKEN> -H 'Content-Type: application/json' -d '{"password":"Str0ngP@ss!"}'
```

## 邮件投递（两种模式）
`app.mail.delivery`：
- `log`（默认）：Thymeleaf 渲染后**落盘** `./var/mail/*.html` 并记 `email_log`，便于本地查看/联调（可配 MailHog）。
- `smtp`：经 `JavaMailSender` 真实外发。配置：
```bash
APP_MAIL_DELIVERY=smtp \
MAIL_HOST=smtp.example.com MAIL_PORT=587 \
MAIL_USERNAME=xxx MAIL_PASSWORD=xxx \
APP_MAIL_FROM=no-reply@yourdomain.com \
APP_ACTIVATION_BASE_URL=https://console.yourdomain.com/activate \
java -jar target/license-platform-1.0.1.jar
```

## 设计要点
- **Saga / 状态机**：`INITIALIZING → ACTIVE`（失败 `FAILED`）；每步落 `audit_log`；DB 步骤 `@Transactional` 原子，
  邮件失败**不回滚**（记 `emailSent=false`，可重发）——生产建议改“事务发件箱(outbox) + 异步投递”。
- **安全**：超管开通时**不设密码**，密码由管理员凭**一次性、限时**激活令牌自设（BCrypt）；
  首次登录强制改密 + 绑定 MFA（`must_change_pwd` / `mfa_bound` 标记预留）。
- **可追溯**：`audit_log`（开通各步骤）、`email_log`（发送状态/落盘路径）。
- **schema 版本化**：Flyway `db/migration/V1__init.sql`；JPA `ddl-auto=validate` 防漂移。

## 切换 PostgreSQL（生产）
1. pom 取消 PostgreSQL 依赖注释；2. `application.yml` 数据源换 PG URL；3. Flyway 自动迁移。
租户级隔离（独立库/Schema/行级 RLS）在开通 Saga 的“资源编排/DB 初始化”步骤按 `isolation` 落地。

## 与前端联调
前端 `web/console` 的 `api/tenant.ts` 将 `USE_MOCK=false`，Vite 已把 `/api` 代理到 `:8080`。
开通对话框提交后即触发本服务发送开通邮件。
