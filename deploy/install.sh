#!/usr/bin/env bash
# CMSSOAS CentOS7 安装/部署脚本（在已构建出 jar 与前端 dist 后执行）
# 用法：sudo bash deploy/install.sh <backend-jar> <web-dist-dir>
set -euo pipefail

JAR="${1:-server/license-platform/target/license-platform-1.0.0.jar}"
WEB="${2:-web/console/dist}"

echo "[1/6] 创建用户与目录"
id cmssoas >/dev/null 2>&1 || useradd -r -s /sbin/nologin cmssoas
mkdir -p /opt/cmssoas/backend /opt/cmssoas/web /etc/cmssoas

echo "[2/6] 部署后端 jar"
install -m 0644 "$JAR" /opt/cmssoas/backend/app.jar
[ -f /etc/cmssoas/backend.env ] || install -m 0640 deploy/cmssoas-backend.env.example /etc/cmssoas/backend.env
chown -R cmssoas:cmssoas /opt/cmssoas /etc/cmssoas

echo "[3/6] 部署前端静态资源"
rm -rf /opt/cmssoas/web/* && cp -r "$WEB"/* /opt/cmssoas/web/

echo "[4/6] 安装 systemd 单元"
install -m 0644 deploy/systemd/cmssoas-backend.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable cmssoas-backend

echo "[5/6] 安装 nginx 站点"
install -m 0644 deploy/nginx/cmssoas.conf /etc/nginx/conf.d/cmssoas.conf
nginx -t

echo "[6/6] 启动服务"
systemctl restart cmssoas-backend
systemctl restart nginx

echo "完成。请编辑 /etc/cmssoas/backend.env 后 systemctl restart cmssoas-backend"
echo "健康检查：curl -s http://127.0.0.1:8080/actuator/health"
