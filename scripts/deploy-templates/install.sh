#!/usr/bin/env bash
# 知联汇学服务器安装脚本（CentOS/Ubuntu，root 运行）
# 用法：在解压后的部署包目录里执行  bash install.sh
set -e

APP_DIR=/opt/zhi-platform
HERE=$(pwd)

echo "== 0. 检查依赖 =="
command -v java  >/dev/null 2>&1 || { echo "[缺少] JDK 17，请先安装"; exit 1; }
command -v nginx >/dev/null 2>&1 || { echo "[缺少] nginx，请先安装"; exit 1; }
command -v mysql >/dev/null 2>&1 || { echo "[缺少] mysql 客户端，请先安装"; exit 1; }

echo "== 1. 复制文件到 $APP_DIR =="
mkdir -p "$APP_DIR/backend" "$APP_DIR/frontend"
cp -f backend/zhi-platform-1.0.0.jar "$APP_DIR/backend/"
cp -f backend/schema.sql "$APP_DIR/backend/"
cp -f backend/neo4j-init.cypher "$APP_DIR/backend/"
cp -rf frontend/. "$APP_DIR/frontend/"
cp -f nginx/zhi-platform.conf /etc/nginx/conf.d/zhi-platform.conf
cp -f systemd/zhi-platform.service /etc/systemd/system/

echo "== 2. 环境变量 =="
if [ ! -f /etc/zhi-platform.env ]; then
  cp .env.example /etc/zhi-platform.env
  echo "!! 请先编辑 /etc/zhi-platform.env 填写数据库/密钥后，重新运行本脚本"
  exit 1
fi

echo "== 3. 初始化 MySQL 数据库（首次执行，可重复） =="
# 数据库密码从环境文件解析
source /etc/zhi-platform.env 2>/dev/null || true
MYSQL_PWD="${SPRING_DATASOURCE_PASSWORD:-}" mysql -u"${SPRING_DATASOURCE_USERNAME:-root}" < backend/schema.sql || echo "[警告] MySQL 初始化失败，请手动检查"

echo "== 4. 启动后端 =="
systemctl daemon-reload
systemctl enable --now zhi-platform || true
systemctl restart zhi-platform
sleep 5
systemctl status zhi-platform --no-pager | head -8 || true

echo "== 5. 启动/重载 nginx =="
nginx -t && (systemctl reload nginx 2>/dev/null || nginx -s reload 2>/dev/null || true)

echo ""
echo "=========================================="
echo "  部署完成！"
echo "  - 前端:  http://<服务器IP>/"
echo "  - 后端:  http://<服务器IP>:8080"
echo "  - Neo4j 数据请自行初始化（见 backend/neo4j-init.cypher）"
echo "  - 修改 /etc/zhi-platform.env 后: systemctl restart zhi-platform"
echo "=========================================="