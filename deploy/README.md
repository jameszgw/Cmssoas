# CMSSOAS 部署目录(deploy/)

本目录集中存放**最新部署说明、数据库脚本、初始化/测试数据、操作与帮助文档**,便于交付与运维。

```
deploy/
├── README.md                     # 本文件:部署总说明(最新)
├── cmssoas-backend.env.example   # 后端环境变量样例(systemd 裸机部署用)
├── install.sh                    # CentOS 一键安装脚本(systemd + nginx)
├── systemd/cmssoas-backend.service
├── nginx/cmssoas.conf
├── sql/                          # 数据库脚本(表结构 / 系统数据 / 测试数据)
│   ├── README.md
│   ├── schema-postgresql.sql     # 全量表结构 + 系统种子(PostgreSQL/H2,自动生成)
│   ├── schema-mysql.sql          # 全量表结构 + 系统种子(MySQL 5.7/8.0,自动生成)
│   ├── init-admin.sql            # 超管账号 admin/8888 + 全量授权(可选,幂等)
│   ├── test-data.sql             # 演示/测试数据(可选)
│   └── generate-schema.sh        # 由 Flyway 迁移重新生成 schema(防漂移)
└── docs/
    ├── 操作说明.md                # 功能操作说明(按模块)
    └── 帮助文档.md                # 帮助 / FAQ
```

> 更完整的运维细节见仓库根 [DEPLOY.md](../DEPLOY.md);各功能设计与接口见根 `docs/`。

---

## 1. 运行环境要求
- **JDK 17+(推荐 21)** — 不支持 Java 8(Spring Boot 3.5 要求 Java 17+)。
- **数据库**:PostgreSQL(生产推荐)/ MySQL 5.7·8.0 / H2(仅开发);均由 CI 真机验证迁移。
- Redis(多实例 nonce,可选)、SMTP(邮件,可选)。
- 框架:Spring Boot 3.5.0 + Vue3。

## 2. 最快上手(Docker Compose,推荐)
```bash
cp .env.example .env          # 试用;生产用 cp .env.prod.example .env 并改密钥/数据库/SMTP
docker compose up -d --build  # 起 PostgreSQL + Redis + 后端 + 前端 + 监控
# 前端 http://localhost   后端 http://localhost:8080   初始账号 admin / 8888
```
- 用 **MySQL**:`docker compose -f docker-compose.yml -f docker-compose.mysql.yml up -d mysql redis backend console`
- 启用**本地大模型(智能客服)**:`docker compose --profile ai up -d` 后 `.env` 设 `AI_ENABLED=true`。
- 环境变量清单见 `.env.example` / `.env.prod.example`。

## 3. 数据库初始化(两种方式,二选一)
**方式 A:应用自动建表(默认,推荐)**
后端启动时 Flyway 自动建表并写入系统种子;`DataInitializer` 自动创建超管 admin/8888 与全量授权。无需手工执行 SQL。
> Profile 决定方言:`prod`=PostgreSQL、`mysql`=MySQL、默认=H2。

**方式 B:纯 SQL 初始化(只建库不依赖应用建表)**
按顺序执行(选对应方言):
```bash
# PostgreSQL
psql -U cmssoas -d cmssoas -f deploy/sql/schema-postgresql.sql
psql -U cmssoas -d cmssoas -f deploy/sql/init-admin.sql        # 可选(应用首启亦会做)
psql -U cmssoas -d cmssoas -f deploy/sql/test-data.sql         # 可选(演示数据)
# MySQL(库须 utf8mb4)
mysql -ucmssoas -p cmssoas < deploy/sql/schema-mysql.sql
mysql -ucmssoas -p cmssoas < deploy/sql/init-admin.sql
mysql -ucmssoas -p cmssoas < deploy/sql/test-data.sql
```
> 用方式 B 时,后端启动建议关闭 Flyway 自动迁移以免重复:`SPRING_FLYWAY_ENABLED=false`(或让 Flyway baseline)。

## 4. 裸机(CentOS systemd + nginx)
```bash
cp deploy/cmssoas-backend.env.example /etc/cmssoas/backend.env   # 改数据库/密钥
sudo bash deploy/install.sh                                      # 装 systemd 服务 + nginx
```
详见 [DEPLOY.md](../DEPLOY.md) 与 `docs/部署-CentOS7.md`。

## 5. 智能客服大模型(可选)
通用 OpenAI 兼容,换厂商只改 `.env` 三项(`AI_BASE_URL/AI_API_KEY/AI_MODEL`):
本地 Ollama(离线)/ 智谱 GLM-4-Flash(免费)/ 通义 qwen-turbo / DeepSeek。
选型与部署见 `docs/智能客服-大模型选型与部署.md`。

## 6. 升级后重新生成 schema 脚本
数据库结构以 Flyway 迁移为准。新增迁移后,执行以下命令同步本目录脚本(防漂移):
```bash
bash deploy/sql/generate-schema.sh
```
