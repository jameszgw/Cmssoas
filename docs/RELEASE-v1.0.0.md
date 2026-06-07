# CMSSOAS v1.0.0 发布说明

> 首个交付版本 · 2026-06-07 · 初始账号 `admin / 8888`
> 配套：[交付与验收清单](交付与验收清单.md) · [自测报告（中）](自测报告.md) · [Self-Test Report (EN)](self-test-report-en.md)

## 概述
CMSSOAS 是面向 Spring Boot 交付物的**代码保护 + License 认证 + 多租户运营平台**整体方案与**可运行参考实现**。本版本完成从「方案文档」到「可运行、可观测、可测试、可交付」的全链路闭环。

## 主要特性
- **代码保护**：类字节码 AES 加密 + 解密密钥与 License 绑定（脱壳与破解 License 强耦合）；ProGuard 混淆已接入构建流水线（`mvn -Pharden`）。
- **License 认证**：服务端 **Ed25519 / 国密 SM2** 双算法签名（可插拔，按 `app.license.sign-algo` 切换）；离线 .lic + 在线（激活 / 心跳 nonce 防重放 / 浮动席位 / 宽限期 / 实时吊销 + CRL）；完整生命周期（续期 / 变更 / 吊销 + 版本号 + 历史 diff）。
- **客户端 SDK**：内置公钥验签（Ed25519/SM2 自动按 `sigAlg` 选择）+ 功能 / 版本 / 有效期门禁；demo 演示「无合法 License / 越权 / 篡改 → 受限」。
- **多租户运营平台**：租户一键开通、Flyway 初始化 DB、超管下发；**开通即发邮件（事务发件箱 + 异步重试）**；一次性激活 + MFA(TOTP)；产品-模块-功能-版本矩阵 + 套餐 SKU + **订阅自动签发 License**。
- **登录与 RBAC**：JWT 鉴权；**三层权限**（菜单 / 按钮 / 接口 `@RequirePerm`）；**el-tree 多态权限树**（无 / 只读 / 编辑 / 完全 + 级联）；用户管理 + 首登强制改密；审计覆盖关键写操作。
- **前端体验**：Vue3 + TS + Element Plus；6 主题换肤、2K-4K 自适应、中英 i18n、业务数据等宽字体与帮助字体区分、贯穿式操作帮助。
- **生产化**：PostgreSQL profile、Redis 可插拔（nonce）、邮件 outbox、Docker Compose 一键起、Prometheus + Grafana 可观测。
- **质量保障**：CI 8 条流水线（后端 / SDK / 签名冒烟 Ed25519+SM2 矩阵 / 混淆 / 前端 / E2E / 汇总 / 发布）；制品与覆盖率上传、tag 触发 Release。

## 测试结果（发布前实测）
- 后端单测 + 集成：**9/9**（鉴权、接口权限、TOTP、SM2、激活公开回归）
- 客户端 SDK：**8/8**（SemVer、Ed25519/SM2 验签、防篡改）
- 端到端 E2E：**7/7**（登录/鉴权、多态权限树、签发、订阅、生命周期）
- 签名链路冒烟：Ed25519 与 SM2 双算法端到端验签均通过

## 制品（随 Release 附件 `cmssoas-artifacts.zip`）
- 后端 / SDK 覆盖率报告（JaCoCo）
- 端到端测试报告（Playwright HTML）
- 代码加固混淆制品（`protected-app-1.0.1-obf.jar`）

## 快速开始
```bash
# 一键起（需 Docker 守护进程）
docker compose up -d --build      # 前端 http://localhost · 后端 :8080 · Grafana :3000
# 本地开发
cd server/license-platform && mvn spring-boot:run        # admin / 8888
cd web/console && npm install && npm run dev
# 国密模式
LICENSE_SIGN_ALGO=sm2 java -jar server/license-platform/target/license-platform-1.0.1.jar
```

## 已知约束
- 演示态私钥落盘（`var/keys`），生产须托管 KMS/HSM；邮件默认 `log` 落盘，配置 SMTP 后真实外发。
- 纯软件保护不能绝对防破解：目标是**抬高破解成本至超过软件价值**，并以「技术 + 在线吊销/水印溯源 + 法律协议」构建综合防线。

## 升级 / 兼容
- 首个版本，无升级路径。数据库 schema 由 Flyway V1–V8 管理，启动自动迁移。

## 校验和
> 由 CI `release` 流水线在生成 Release 时附带制品；如需手动校验，可对附件执行 `sha256sum`。
