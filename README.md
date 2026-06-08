# Codeman

[![CI](https://github.com/jameszgw/cmssoas/actions/workflows/ci.yml/badge.svg)](https://github.com/jameszgw/cmssoas/actions/workflows/ci.yml)
![backend tests](https://img.shields.io/badge/backend%20tests-9%2F9-brightgreen)
![sdk tests](https://img.shields.io/badge/sdk%20tests-8%2F8-brightgreen)
![e2e](https://img.shields.io/badge/e2e-7%2F7-brightgreen)
![sign](https://img.shields.io/badge/sign-Ed25519%20%7C%20SM2-blue)
![coverage](https://img.shields.io/badge/coverage-JaCoCo%20(CI%20artifact)-informational)

**C**ode **M**anagement **S**ecurity **S**ystem · Obfuscation & Authorization Scheme
（Spring Boot 代码保护与 License 认证及多租户运营平台 —— 解决方案）

针对基于 Spring Boot 的 jar 交付物，制定一套覆盖**防逆向代码保护**、**离线/在线 License 认证**、
**多租户可运营授权平台（含前后端）** 的完整解决方案。每一部分均包含设计思路、技术选型对比、潜在风险与应对措施。

## 方案文档（`docs/`）

> 📚 **文档中心（HTML，UTF-8，全部 md 的网页版）**：[docs/index.html](docs/index.html) —— 设计方案/功能说明/部署/验收文档的网页版，与帮助中心互链；由 `node scripts/build-docs.mjs` 生成。
> 🎉 **官宣页（可直接浏览器打开）**：[web/landing/index.html](web/landing/index.html) —— 产品总览、三大子系统、功能矩阵、快速开始。
> 🧭 **操作帮助中心（HTML，按流程 / 按模块，可搜索）**：[web/landing/help.html](web/landing/help.html)（中文）· [help-en.html](web/landing/help-en.html)（EN）—— 全功能逐步操作、所需权限/参数、提示与常见问题；已内嵌控制台顶栏「❔ 帮助中心」。
> 📖 **使用手册（从部署到应用）**：[docs/使用手册-从部署到应用.md](docs/使用手册-从部署到应用.md) —— 部署、初始化、逐模块操作、SDK 接入、运维全流程。
>
> 🧭 **会话交接 / 接手必读**：[docs/HANDOFF.md](docs/HANDOFF.md)（中文) · [docs/HANDOFF-en.md](docs/HANDOFF-en.md)（English）——当前状态、架构约定、关键决策、待办。
>
> 👉 **主文档（推荐先读）**：[00-完整解决方案（分步骤·含前后端）](docs/00-完整解决方案(分步骤·含前后端).md)
> 按 20 个实施步骤推进，**每步统一含【设计思路 / 技术选型对比 / 潜在风险 / 应对措施】并标注前端/后端落点**。
> 下列 01–07 为各专题的细节附录。

| 文档 | 内容 |
|------|------|
| [00-完整解决方案（分步骤·含前后端）](docs/00-完整解决方案(分步骤·含前后端).md) | **主文档**：20 步骤，每步四要素 + 前后端落点 |
| [01-总体方案与架构](docs/01-总体方案与架构.md) | 目标、总体架构、组件职责、端到端流程、技术栈总表、风险总览 |
| [02-代码保护方案](docs/02-代码保护方案.md) | 纵深防御分层、混淆/加密/Native 选型对比、与 License 联动解密、反调试/反 dump、风险与应对 |
| [03-License认证方案](docs/03-License认证方案.md) | 签名算法选型、License 模型、离线/在线流程、防时间回拨/防共享、生命周期与版本/历史追溯、SDK 设计 |
| [04-多租户运营平台](docs/04-多租户运营平台.md) | 隔离策略选型、租户开通与 DB 初始化、超管下发、产品-模块-功能-版本授权模型、ER、前后端架构 |
| [05-安全密钥与部署](docs/05-安全密钥与部署.md) | KMS/HSM 密钥管理、密钥分级轮换、通道安全、部署拓扑与高可用 |
| [06-实施路线图与风险总览](docs/06-实施路线图与风险总览.md) | 分阶段实施路线图、全局风险矩阵、验收指标、现实边界声明 |
| [07-小厂离线授权现实方案](docs/07-小厂离线授权现实方案.md) | 硬件指纹为何对小厂不现实；签名期限/软节点锁/在线自助激活/现成方案等低成本替代 |

## 三大子系统一览

1. **代码保护**：ProGuard/Allatori 混淆 + Xjar/ClassFinal 加密套壳 + 关键逻辑 JNI/GraalVM 原生化 + 反调试/反 dump，且**解密密钥依赖合法 License 派生**。
2. **License 认证**：Ed25519/SM2 非对称签名（私钥不出域）；离线（指纹绑定 .lic）+ 在线（心跳/吊销/浮动席位/宽限期）；完整生命周期、版本号与历史追溯。
3. **多租户平台**：租户一键开通、Flyway 初始化 DB 与种子数据、超管安全下发；"产品—模块—功能—版本"授权模型驱动签发；Vue3 运营控制台 + Spring Boot 后端。

> ⚠️ 现实边界：纯软件保护无法做到绝对不可破解，本方案目标是**抬高破解成本至超过软件价值**，并以"技术 + 运营吊销/溯源 + 法律协议"构建综合防线。

## 可运行实现（已落地并自测）

除方案文档外，本仓库附带**可运行的参考实现**，全链路已端到端自测（见 [自测报告](docs/自测报告.md) / [Self-Test Report (EN)](docs/self-test-report-en.md)）。

| 模块 | 路径 | 说明 |
|------|------|------|
| 运营后端 | `server/license-platform` | Spring Boot：开通/激活/MFA、License 签发与生命周期、在线授权、产品/套餐/订阅、RBAC、审计、可观测 |
| 运营前端 | `web/console` | Vue3 + TS + Element Plus：换肤/2K-4K/中英 i18n/操作帮助 |
| 客户端 SDK | `sdk/license-sdk` | Ed25519 / 国密 SM2 验签 + 功能/版本门禁 |
| 代码保护示例 | `examples/protected-app` | 类加密 + 解密密钥与 License 绑定 + ProGuard 混淆 |
| 部署/可观测 | `docker-compose.yml`、`infra/` | PostgreSQL + Redis + Prometheus + Grafana |

```bash
# 后端（默认 H2，开箱即跑；国密模式加 LICENSE_SIGN_ALGO=sm2）
cd server/license-platform && mvn spring-boot:run        # http://localhost:8080
# 前端（初始账号 admin / 8888）
cd web/console && npm install && npm run dev             # http://localhost:5173
```

一键容器化（生产）：复制环境模板后启动——
```bash
cp .env.example .env        # 试用；或 cp .env.prod.example .env（生产）
docker compose up -d --build
```
- 环境变量(JWT/数据库/邮件/CORS/支付·发票渠道/智能客服大模型)见 `.env.example`、`.env.prod.example`。
- **数据库**：H2(开发) / **PostgreSQL(生产推荐)** / **MySQL 5.7 · 8.0(`profile=mysql`，CI 真机矩阵验证)**，见 [DEPLOY.md](DEPLOY.md#数据库支持)。
- **运行环境**：JDK **17+(推荐 21)**;不支持 Java 8(Spring Boot 3.x 要求 Java 17+)。
- **智能客服大模型**选型与部署(API 最省 / 本地 Ollama 离线)见 [docs/智能客服-大模型选型与部署.md](docs/智能客服-大模型选型与部署.md)。

## 界面预览

| 在线授权监控 | 角色权限（el-tree 多态选择） |
|---|---|
| ![在线监控](web/console/shots/st-01-在线监控.png) | ![角色权限多态树](web/console/shots/st-02-角色权限多态树.png) |

| 用户管理 | 首次登录强制改密 |
|---|---|
| ![用户管理](web/console/shots/st-03-用户管理.png) | ![首登强制改密](web/console/shots/st-07-首登强制改密.png) |

| License（超管：全操作按钮） | License（只读角色：签发/吊销按钮隐藏） |
|---|---|
| ![admin全按钮](web/console/shots/st-04-License-admin全按钮.png) | ![VIEWER按钮隐藏](web/console/shots/st-05-License-VIEWER按钮隐藏.png) |

| 套餐订阅（订阅自动签发 License） | 总览（暗夜主题 + 英文 i18n） |
|---|---|
| ![套餐订阅](web/console/shots/plan-02-套餐订阅-真实-2K.png) | ![暗夜英文](web/console/shots/st-06-总览-暗夜-英文.png) |
