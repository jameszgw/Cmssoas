# CentOS7 部署指南（git + GitLab + Nacos + Jenkins）

本指南面向自建栈：**CentOS7 + GitLab(代码/CI) + Nacos(配置中心) + Jenkins(流水线)**。

> ⚠️ CentOS7 已于 2024-06 EOL,建议至少打全补丁,或迁移 Rocky/Alma Linux 9。JDK21(Temurin)依赖 glibc≥2.17,CentOS7(glibc 2.17)可运行。

## 1. 基础环境
```bash
# JDK 21（Temurin），解压到 /opt/jdk-21
curl -fsSL -o /tmp/jdk21.tgz https://github.com/adoptium/temurin21-binaries/releases/latest/download/OpenJDK21U-jdk_x64_linux_hotspot.tar.gz
mkdir -p /opt/jdk-21 && tar -xzf /tmp/jdk21.tgz -C /opt/jdk-21 --strip-components=1

# Node 22（构建前端，可只在 Jenkins/构建机安装）
# PostgreSQL 与 Redis（可 yum 安装或容器化）
yum install -y nginx
```

## 2. 中间件
- **PostgreSQL**：建库 `cmssoas`、账号与密码;首次启动后端会自动执行 Flyway 迁移(V1–V8)。
- **Redis**：用于心跳 nonce 防重放(多实例水平扩展)。
- **Nacos**：配置中心,创建命名空间(dev/test/prod)与配置 `cmssoas.yml`,放置 DB/Redis/SMTP/JWT 密钥/`app.license.sign-algo` 等。

## 3. 构建（GitLab CI 或 Jenkins）
- **GitLab CI**：仓库根 `.gitlab-ci.yml` 已提供(build/test/e2e/package/release + 签名冒烟矩阵)。
- **Jenkins**：仓库根 `Jenkinsfile`(声明式 pipeline)。需在 Jenkins 配置 JDK21/Maven/NodeJS 工具,凭据(DB/SMTP/registry)用 Jenkins Credentials 注入为环境变量。
- 产物：`server/license-platform/target/license-platform-1.0.1.jar` 与 `web/console/dist`。
- 国密构建：`mvn -Pnacos ...`(接 Nacos)与 `LICENSE_SIGN_ALGO=sm2`(SM2 签名)按需组合。

## 4. 部署（脚本）
```bash
# 在仓库根、已产出 jar 与 dist 后：
sudo bash deploy/install.sh \
  server/license-platform/target/license-platform-1.0.1.jar \
  web/console/dist
# 编辑环境变量后重启
sudo vi /etc/cmssoas/backend.env      # 见 deploy/cmssoas-backend.env.example
sudo systemctl restart cmssoas-backend
curl -s http://127.0.0.1:8080/actuator/health
```
- 后端：`systemd` 单元 `cmssoas-backend`(`deploy/systemd/`),环境变量 `/etc/cmssoas/backend.env`。
- 前端：nginx 托管 `dist` + 反代 `/api`、`/sdk`(`deploy/nginx/cmssoas.conf`)。

## 5. 接入 Nacos（可选）
```bash
# 构建时启用 nacos profile（引入 nacos-config 依赖）
mvn -Pnacos -DskipTests package
# 运行时
SPRING_PROFILES_ACTIVE=nacos,prod \
NACOS_ADDR=10.0.0.10:8848 NACOS_NAMESPACE=prod \
java -jar app.jar
```
配置优先级：Nacos `cmssoas.yml` > 本地 `application-prod.yml` > `application.yml`。敏感项(密钥/密码)建议只放 Nacos 并加密。

## 6. 高可用与安全（建议）
- 后端多实例 + nginx/LB；启用 Redis 后 nonce 跨实例一致。
- 私钥托管 KMS/HSM(当前演示落盘 `var/keys`);Nacos 配置鉴权 + 传输加密。
- 仅内网放行 `/actuator/**`;对外启用 HTTPS(nginx 证书)。
- 数据库主从 + 定时备份;日志/指标接入(Actuator `/actuator/prometheus` + Prometheus/Grafana,见 `infra/`)。

## 6.1 多实例 + Nacos 注册发现
- 以 `-Pnacos` 构建,`SPRING_PROFILES_ACTIVE=nacos,prod` 启动,实例自动注册到 Nacos。
- 同一服务起多实例(不同 `--server.port` / 不同主机),nginx `upstream` 或网关按服务名负载均衡：
  ```nginx
  upstream cmssoas_backend { server 10.0.0.11:8080; server 10.0.0.12:8080; }
  # location /api/ { proxy_pass http://cmssoas_backend; }
  ```
- **多实例一致性**：心跳 nonce 与限流计数需共享 Redis(`APP_REDIS_ENABLED=true`)。
- **逻辑拆分(可选)**：签发/运营服务(`SERVICE_NAME=license-platform`) 与 在线校验服务(`SERVICE_NAME=license-edge`,仅暴露 `/sdk/**`)用不同服务名分别扩缩容。

## 6.2 私钥与告警
- 私钥经 KMS/Vault 注入(`APP_LICENSE_ED25519_PRIV/PUB`),私钥不落盘,详见 `docs/密钥管理-KMS.md`。
- 到期/异常告警可推送企业微信群机器人：设置 `ALERT_WECOM_WEBHOOK`。

## 7. Docker 方式（替代裸机）
仓库根 `docker-compose.yml` 一键起 PostgreSQL/Redis/后端/前端 + Prometheus/Grafana;CentOS7 装 docker-ce + compose 插件即可 `docker compose up -d --build`。
