#!/usr/bin/env bash
# CODEMAN CentOS7 安装/部署脚本（在已构建出 jar 与前端 dist 后执行）
# 用法：sudo bash deploy/install.sh <backend-jar> <web-dist-dir>
set -euo pipefail

JAR="${1:-server/license-platform/target/license-platform-1.0.1.jar}"
WEB="${2:-web/console/dist}"

echo "[1/6] 创建用户与目录"
id codeman >/dev/null 2>&1 || useradd -r -s /sbin/nologin codeman
mkdir -p /opt/codeman/backend /opt/codeman/web /etc/codeman

echo "[2/6] 部署后端 jar"
install -m 0644 "$JAR" /opt/codeman/backend/app.jar
[ -f /etc/codeman/backend.env ] || install -m 0640 deploy/codeman-backend.env.example /etc/codeman/backend.env
chown -R codeman:codeman /opt/codeman /etc/codeman

echo "[3/6] 部署前端静态资源"
rm -rf /opt/codeman/web/* && cp -r "$WEB"/* /opt/codeman/web/

echo "[4/6] 安装 systemd 单元"
install -m 0644 deploy/systemd/codeman-backend.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable codeman-backend

echo "[5/6] 安装 nginx 站点"
install -m 0644 deploy/nginx/codeman.conf /etc/nginx/conf.d/codeman.conf
nginx -t

echo "[6/6] 启动服务"
systemctl restart codeman-backend
systemctl restart nginx

echo "完成。请编辑 /etc/codeman/backend.env 后 systemctl restart codeman-backend"
echo "健康检查：curl -s http://127.0.0.1:8080/actuator/health"
