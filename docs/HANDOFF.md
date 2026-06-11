# CODEMAN 会话交接(Handoff)

> 供下一个会话快速接续。记录当前状态、架构约定、关键决策与"踩坑修复",以及候选下一步。
> English version: [HANDOFF-en.md](HANDOFF-en.md)

## 0. 速览
- **项目**:CODEMAN —— Spring Boot 代码保护 + License 认证 + 多租户运营平台(后端 + Vue3 控制台 + 客户端 SDK + 代码保护示例)。
- **开发分支**:`claude/dreamy-faraday-qhs9js`(当前会话;沿用约定:**禁止**改 `main`)。历史分支 `claude/loving-einstein-Pp4cA` 已合并。GitHub 仓库 slug 仍为 `jameszgw/cmssoas`(**未改名**,徽章/链接保持此 slug)。
- **最新提交**:`63830a4`(在线代码加固)。CI 全绿(run #56,11 作业 success;tag 作业 skipped 属正常)。
- **栈/版本**:Spring Boot **3.5.0**,Java **21**(不支持 Java 8),Maven;Vue3+TS+Element Plus+vite;groupId/根包 **com.codeman**;版本 **1.0.1**;License **GPLv3**。
- **数据库**:H2(开发默认,`MODE=PostgreSQL`)/ PostgreSQL(生产,`profile=prod`)/ MySQL 5.7·8.0(`profile=mysql`,`db/mysql` 方言脚本,CI 真机矩阵验证)。
- **初始账号**:`admin / 8888`(首登强制改密);默认用户初始密码 `Codeman@123`。
- **构建命令**:后端 `cd server/license-platform && mvn test`(全套);前端 `cd web/console && npm run build`。

## 1. 命名/品牌约定(重要,改名已彻底完成)
- Java 包/坐标:`com.codeman.*`;groupId `com.codeman`;三模块 version `1.0.1`。
- 品牌字样 `CODEMAN`;DB 名 `codeman`;邮箱域 `@codeman.com`;JWT 默认串 `codeman-dev-...`;前端 localStorage 键 `codeman.*`(token/theme/locale/portal/help);npm 包 `codeman-console`。
- **唯一例外**:GitHub 仓库 URL 仍 `jameszgw/cmssoas`(README 徽章已保持);工作目录物理路径仍 `/home/user/Cmssoas`(不可改)。
- 全仓 `cmssoas/CMSSOAS/Cmssoas` 残留 = 0(LICENSE 文件为 GPLv3 原文,未动)。

## 2. 已交付的业务模块(后端包 `com.codeman.platform.*`)
overview / tenant(开通/激活/MFA) / license(签发·续期·变更·吊销·**签名CRL**·到期自动停用;Ed25519+SM2) / online(SDK 在线通道) / catalog(产品·套餐·订阅) / **customer(客户主数据+客户360)** / billing(**支付收款(人工确认)** + **电子发票**) / **contract(自建电子签·哈希存证)** / **notice(须知+用户授权)** / **cs(智能客服,OpenAI 兼容,可降级)** / **portal(租户自助门户)** / **harden(在线代码加固)** / **cmreport(CmReport 报表平台产品线授权:RSA SHA256 产品格式签发+版本矩阵+订阅路由,见 docs/功能-CmReport授权集成.md)** / rbac / mail(outbox) / alert / common。
- 前端菜单(权限点):overview, tenant, license, online, catalog, plan, **customer**, billing, **contract**, **notice**, **cs**, **harden**, **tenant:portal**(自助门户管理), audit, role:view, user:view。
- 公开页(无 ops 鉴权):`/activate/:token`(激活)、`/portal` + `/portal/home`(租户门户)。
- 公开后端端点(`/pub/**`,JwtAuthFilter 放行):`/pub/notices/active`、`/pub/consents`、`/pub/payments/notify/{channel}`、`/pub/portal/login|overview`、`/pub/crl`、`/pub/license/public-keys`。

## 3. 数据库迁移
- PG/H2:`server/.../db/migration/V1..V18`。MySQL 方言:`server/.../db/mysql/V1..V18`(机械转换:`IDENTITY→AUTO_INCREMENT`、`TIMESTAMP→DATETIME`、布尔默认 `1/0`、每表 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`)。
- **新增迁移后必做**:① 在 `db/migration` 加 `Vn`;② 在 `db/mysql` 加同号方言版(同样转换);③ 运行 `bash deploy/sql/generate-schema.sh` 重生成 `deploy/sql/schema-*.sql`;④ 更新 `MysqlMigrationTest`/`MysqlRealMigrationTest` 里的迁移计数断言(当前 **18**)。
- 最近迁移:V15 customer、V16 tenant_portal、V17 tax_invoice、V18 harden、**V19 cmreport_catalog(CmReport 产品/套餐 + license.signature 扩 512)**;迁移计数断言现为 **19**。

## 4. "通用·不绑定厂商·可降级"的 provider 抽象(本项目核心范式)
- 智能客服:`ChatProvider`+`OpenAiCompatProvider`(纯 JDK HttpClient,SSE);未配置 `app.ai.*` 则降级知识库(关键词召回 `knowledge/faq.md`)。配 `AI_BASE_URL/AI_API_KEY/AI_MODEL` 即接 GLM-4-Flash/通义/DeepSeek/Ollama。
- 支付:`PaymentProvider`+`MockPaymentProvider`(默认人工确认到账;`app.pay.provider`)。
- 电子发票:`EInvoiceProvider`+`MockEInvoiceProvider`(`app.einvoice.provider`)。
- 代码加固:`HardenProvider`(OBFUSCATE=ProGuard 库内调用;ENCRYPT_BIND=类加密+License;FATJAR_ENCRYPT=类加密+口令)。预留 Allatori/ClassFinal/Xjar。

## 5. 在线代码加固(最近一次大功能,细节)
- 与"构建/打包"(`examples/protected-app`,`mvn -Pharden`)**并存**;按租户 `HardenConfig`(BUILD/ONLINE/BOTH)+ 任务级覆盖;BUILD 模式拒绝在线加固。
- 流程:`POST /api/harden/jobs`(multipart 上传)→ 异步流水线(按勾选技术顺序)→ `GET /api/harden/jobs/{id}/download`。配置 `app.harden.work-dir`(compose 已挂持久卷 `harden`)、`spring.servlet.multipart`。
- 运行期注入纯 JDK `com.codeman.platform.harden.runtime.HardenLauncher/HardenClassLoader`(AES-256-GCM 解密加载;密钥 SHA-256 派生:LICENSE_BIND 取绑定 License 的 `.lic` 原文,PASSPHRASE 取口令)。
- 产物运行:`java -Dharden.license=<.lic> -jar x.jar`(绑定)/ `java -Dharden.key=<口令> -jar x.jar`(口令)。
- **关键修复**:proguard-base 拉入 log4j-core 与 Spring 的 log4j-to-slf4j 冲突 → 加 `src/main/resources/log4j2.component.properties` 强制 `Log4jContextFactory`;ProGuard library jars 仅用 `java.base/java.logging`(避免 java.desktop 超大拖慢);`HardenService.run` 捕获 `Throwable`(防 Error 静默挂死任务)。

## 6. 测试与 CI
- 后端测试(`mvn test`):约 27 用例(1 个 `MysqlRealMigrationTest` 仅 CI 真机跑、本地跳过)。关键:`*IntegrationTest`(rbac/features:notice·payment·tax·customer·portal·licenseLifecycle·harden)、`MysqlMigrationTest`(H2 MySQL 模式)、`KnowledgeBaseTest`、`Sm2SignatureServiceTest`、`TotpTest`、`TenantSchemaServiceTest`。
- CI `.github/workflows/ci.yml`:backend / sdk / sign-smoke(ed25519,sm2) / harden(ProGuard) / frontend / e2e(Playwright) / **mysql 矩阵 [8.0, 5.7] 真机迁移** / ci-summary / release(仅 tag)。
- **真机验证手法**(沙箱无常驻 `&`):用 Bash 工具 `run_in_background:true` 跑 `java -jar`;`until grep "Tomcat started on port 8080" log` 等待;`mcp__github__actions_*` + `mcp__github__get_job_logs` 查 CI(list 输出过大时存盘后用 python 解析)。

## 7. 已知限制 / 待办候选(未做)
- 在线加固:ProGuard 默认 keep 规则保守(对任意 jar 通用),强混淆需用户自带 keep 规则;胖包(BOOT-INF/lib 嵌套)整体加密未做(当前按包前缀加密业务类)。Xjar/Allatori/ClassFinal 适配器**预留未实现**。
- 支付/电子签/电子发票:默认 mock/人工/沙箱;真实渠道(微信支付/支付宝/Stripe、航信/百望、e签宝/法大大)适配器未实现(抽象已就位)。
- 其它增强候选:用量计费(metering)、营收对账报表、人工客服坐席工作台、KMS/HSM 实接、HA/备份恢复/蓝绿、PostgreSQL 真机 CI(目前仅 MySQL 真机矩阵)。
- 运维:`tag v*` 推送被环境 403 拦(需用户在 GitHub UI 打 tag);PR 未建(用户未要求)。

## 8. 约束(贯穿全程)
- 私钥不落明文(env/Nacos/KMS 注入);智能客服不外发密钥/隐私。
- 仅在 `claude/loving-einstein-Pp4cA` 开发并推送;`git push -u origin <branch>` 失败按 2/4/8/16s 退避重试;**不建 PR、不推 tag**除非明确要求。
- 文档优先 `docs/`(功能/设计)与 `deploy/`(部署/操作/帮助);改动后保持 README/DEPLOY/deploy 索引同步;代码与迁移含中文注释。

## 9. 常用路径
- 后端:`server/license-platform`(`src/main/java/com/codeman/platform/<模块>`,`src/main/resources/{application*.yml,db/migration,db/mysql,knowledge,log4j2.component.properties}`)。
- 前端:`web/console/src/{views,api,layouts/DefaultLayout.vue,router/index.ts,i18n/locales/{zh-CN,en-US}.ts,components}`;截图 `web/console/shots/`。
- SDK/示例:`sdk/license-sdk`、`examples/protected-app`(ProGuard `proguard.pro` + `-Pharden`)。
- 文档:`docs/功能-*.md`、`docs/智能客服-大模型选型与部署.md`、`docs/02-代码保护方案.md`;`deploy/README.md`、`deploy/sql/*`、`deploy/docs/{操作说明,帮助文档,README}.md`;`.env.example`、`.env.prod.example`、`docker-compose.yml`、`docker-compose.mysql.yml`。
