#!/usr/bin/env bash
# ============================================================================
# 知联汇学（Knowledge Graph）一键部署脚本
#
# 支持系统：
#   - Ubuntu 22.04 / 24.04、Debian 12 ......... 全自动
#   - CentOS/RHEL/Rocky/AlmaLinux 9 ............ 自动装 JDK/Maven/Nginx/Node/MariaDB，
#                                                Neo4j 需手动安装后重跑
#
# 用法（任选其一，需 root / sudo）：
#   1) 全新服务器（自动 clone 源码并构建）：
#        sudo bash install.sh
#   2) 已 clone 源码（推荐）：
#        cd ~/zhi-platform-src && sudo bash install.sh
#   3) 已上传 deploy 部署包：
#        cd deploy && sudo bash install.sh
#
# 可选环境变量（不设则自动生成随机值并写入 /etc/zhi-platform.env）：
#   MYSQL_PASSWORD   MySQL root 密码
#   NEO4J_PASSWORD   Neo4j neo4j 密码
#   JWT_SECRET       JWT 签名密钥（>=32 字节）
#   MODELSCOPE_API_KEY / PADDLE_OCR_TOKEN   云端 AI 密钥
#   ADMIN_PASSWORD      初始管理员密码（默认 123456）
#   CLEAN_BUILD         设为 1 强制 mvn clean 全量构建（默认增量，构建中断后可续跑）
#   SERVER_NAME      Nginx 域名或 IP（默认自动取服务器 IP）
# ============================================================================
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/zhi-platform}"
ENV_FILE="/etc/zhi-platform.env"
REPO_URL="https://github.com/wu-xing-ai/Knowledge-Graph.git"

log()  { echo -e "\033[32m[OK]\033[0m $*"; }
info() { echo -e "\033[36m[..]\033[0m $*"; }
warn() { echo -e "\033[33m[!]\033[0m $*"; }
die()  { echo -e "\033[31m[错误]\033[0m $*" >&2; exit 1; }
# 安全加载 env 文件：逐行解析，不当作 bash 执行（值里含引号/$/反引号都不会报错）
load_env_file() {
  local file="$1" key val
  [ -f "$file" ] || return 0
  while IFS='=' read -r key val; do
    key="${key#"${key%%[![:space:]]*}"}"   # 去掉行首空格
    [ -n "$key" ] || continue
    case "$key" in
      \#*|\;*) continue ;;                  # 跳过注释
    esac
    val="${val%\"}"; val="${val#\"}"        # 去掉值首尾的引号
    val="${val%\'}"; val="${val#\'}"
    export "$key"="$val"
  done < <(sed 's/\r$//' "$file")           # 顺带处理 Windows CRLF
}

# ---------- 1. 权限 / 系统检测 ----------
[ "$(id -u)" -eq 0 ] || die "请用 root 运行：sudo bash install.sh"
if [ -f /etc/os-release ]; then . /etc/os-release; fi
case "${ID:-}" in
  ubuntu|debian)          DISTRO="debian" ;;
  centos|rhel|rocky|almalinux|fedora) DISTRO="rhel" ;;
  *) die "暂不支持的系统：${ID:-unknown}，建议使用 Ubuntu 22.04/24.04" ;;
esac
info "系统：${PRETTY_NAME:-$ID}"

# 预装最小工具（git/openssl/curl/wget 用于后面的 clone、生成密钥、装仓库）
pre_install() {
  local missing=""
  command -v git     >/dev/null 2>&1 || missing="$missing git"
  command -v openssl >/dev/null 2>&1 || missing="$missing openssl"
  command -v curl    >/dev/null 2>&1 || missing="$missing curl"
  command -v wget    >/dev/null 2>&1 || missing="$missing wget"
  if [ -n "$missing" ]; then
    info "安装基础工具：$missing"
    if [ "$DISTRO" = "debian" ]; then apt-get update -y && apt-get install -y $missing
    else dnf install -y $missing; fi
  fi
}
pre_install

# ---------- 2. 生成 / 读取生产密钥 ----------
JWT_SECRET="${JWT_SECRET:-}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-}"
if [ ! -f "$ENV_FILE" ] && [ -f "./.env" ]; then
  info "发现仓库根目录 .env，复制为生产配置 $ENV_FILE（注意：不要将 .env 提交到 Git）"
  cp -f "./.env" "$ENV_FILE"
  sed -i "s/\r$//" "$ENV_FILE"  # Windows 上传的 .env 可能是 CRLF，转成 LF 防止解析出错
  chmod 600 "$ENV_FILE"
fi
if [ -f "$ENV_FILE" ]; then
  info "发现 $ENV_FILE，沿用现有配置"
  load_env_file "$ENV_FILE"
  MYSQL_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$MYSQL_PASSWORD}"
  NEO4J_PASSWORD="${SPRING_NEO4J_AUTHENTICATION_PASSWORD:-$NEO4J_PASSWORD}"
  JWT_SECRET="${JWT_SECRET:-}"
else
  JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 | tr -d '=+/')}"
  MYSQL_PASSWORD="${MYSQL_PASSWORD:-$(openssl rand -base64 18 | tr -d '=+/')}"
  NEO4J_PASSWORD="${NEO4J_PASSWORD:-$(openssl rand -base64 18 | tr -d '=+/')}"
  cat > "$ENV_FILE" <<EOF
# ===== 知联汇学生产配置（含自动生成的随机密码，请勿外传/提交仓库）=====
JWT_SECRET=${JWT_SECRET}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/zhi_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD}
SPRING_NEO4J_URI=bolt://localhost:7687
SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
SPRING_NEO4J_AUTHENTICATION_PASSWORD=${NEO4J_PASSWORD}
MODELSCOPE_API_KEY=${MODELSCOPE_API_KEY:-}
PADDLE_OCR_TOKEN=${PADDLE_OCR_TOKEN:-}
EOF
  chmod 600 "$ENV_FILE"
  info "已生成 $ENV_FILE（JWT/数据库/Neo4j 均为随机密码）"
fi
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"
[ -n "$MYSQL_PASSWORD" ] || die "$ENV_FILE 中 SPRING_DATASOURCE_PASSWORD 为空，请填写后重跑"
[ -n "$NEO4J_PASSWORD" ] || die "$ENV_FILE 中 SPRING_NEO4J_AUTHENTICATION_PASSWORD 为空，请填写后重跑"

# ---------- 3. 定位源码 / 部署包 ----------
SRC=""; MODE=""
find_src() {
  if [ -f "backend/pom.xml" ]; then SRC="$PWD"; MODE="source"; return; fi
  if [ -f "backend/zhi-platform-1.0.0.jar" ]; then SRC="$PWD"; MODE="package"; return; fi
  for cand in "$HOME/zhi-platform-src" "/opt/zhi-platform-src"; do
    if [ -f "$cand/backend/pom.xml" ]; then SRC="$cand"; MODE="source"; return; fi
  done
  info "未找到源码/部署包，开始从 GitHub 克隆..."
  git clone --depth 1 "$REPO_URL" /opt/zhi-platform-src
  SRC="/opt/zhi-platform-src"; MODE="source"
}
find_src
info "构建模式：$MODE（目录：$SRC）"

# ---------- 4. 安装依赖 ----------
install_deps() {
  if [ "$DISTRO" = "debian" ]; then
    export DEBIAN_FRONTEND=noninteractive
    info "安装系统依赖（JDK21 / Maven / Nginx / MySQL / Node / Neo4j）..."
    apt-get update -y
    apt-get install -y curl wget git unzip vim gnupg ca-certificates openssl sudo openjdk-21-jdk maven nginx mysql-server apache2-utils
    if ! command -v node >/dev/null 2>&1; then
      info "安装 Node.js 20（NodeSource）..."
      curl -fsSL https://deb.nodesource.com/setup_20.x | bash - || true
      apt-get install -y nodejs
    fi
    if ! command -v neo4j >/dev/null 2>&1; then
      info "安装 Neo4j Community（官方 apt 源）..."
      mkdir -p /etc/apt/keyrings
      wget -q -O - https://debian.neo4j.com/neotechnology.gpg.key | gpg --dearmor -o /etc/apt/keyrings/neotechnology.gpg
      chmod a+r /etc/apt/keyrings/neotechnology.gpg
      echo "deb [signed-by=/etc/apt/keyrings/neotechnology.gpg] https://debian.neo4j.com stable latest" > /etc/apt/sources.list.d/neo4j.list
      apt-get update -y
      apt-get install -y neo4j
    fi
  else
    info "安装 RHEL 系依赖（JDK21 / Maven / Nginx / MariaDB / Node）..."
    dnf install -y curl wget git unzip vim openssl sudo java-21-openjdk-devel maven nginx mariadb-server httpd-tools
    if ! command -v node >/dev/null 2>&1; then
      dnf module enable -y nodejs:20 || true
      dnf install -y nodejs || true
    fi
    if ! command -v neo4j >/dev/null 2>&1; then
      warn "Neo4j 未安装。请按官方文档手动安装后重新运行本脚本："
      warn "  https://neo4j.com/docs/operations-manual/current/installation/"
      exit 1
    fi
  fi
}
install_deps

# ---------- 5. 构建 ----------
JAR=""; SCHEMA=""; NEO4J_INIT=""
if [ "$MODE" = "source" ]; then
  MVN_CLEAN=""
  [ "${CLEAN_BUILD:-0}" = "1" ] && MVN_CLEAN="clean"
  info "构建后端（Maven ${MVN_CLEAN:-增量} package -DskipTests）..."
  ( cd "$SRC/backend" && mvn $MVN_CLEAN package -DskipTests -ntp )
  info "构建前端（npm install && npm run build）..."
  ( cd "$SRC/frontend" && npm install --no-audit --no-fund && npm run build )
  JAR="$SRC/backend/target/zhi-platform-1.0.0.jar"
  SCHEMA="$SRC/backend/src/main/resources/schema.sql"
  NEO4J_INIT="$SRC/backend/src/main/resources/neo4j-init.cypher"
else
  JAR="$SRC/backend/zhi-platform-1.0.0.jar"
  SCHEMA="$SRC/backend/schema.sql"
  NEO4J_INIT="$SRC/backend/neo4j-init.cypher"
fi
[ -f "$JAR" ] || die "未找到后端 jar：$JAR"
log "产物就绪：$JAR"

# ---------- 6. MySQL 初始化 ----------
setup_mysql() {
  command -v mysql >/dev/null 2>&1 || die "MySQL 未安装"
  systemctl enable --now mysql 2>/dev/null || systemctl enable --now mariadb 2>/dev/null || service mysql start 2>/dev/null || service mariadb start 2>/dev/null || true
  for i in $(seq 1 20); do
    if mysql -uroot -e "SELECT 1" >/dev/null 2>&1; then break; fi
    sleep 1
  done
  if mysql -uroot -p"$MYSQL_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; then
    log "MySQL root 密码已生效"
  elif mysql -uroot -e "SELECT 1" >/dev/null 2>&1; then
    mysql -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$MYSQL_PASSWORD'; FLUSH PRIVILEGES;" \
      || mysql -uroot -e "SET PASSWORD FOR 'root'@'localhost' = PASSWORD('$MYSQL_PASSWORD'); FLUSH PRIVILEGES;"
    log "已设置 MySQL root 密码"
  else
    die "无法连接 MySQL，请手动执行 sudo mysql 设置 root 密码后重跑本脚本"
  fi
  mysql -uroot -p"$MYSQL_PASSWORD" < "$SCHEMA" || die "MySQL 初始化失败：$SCHEMA"
  log "MySQL 初始化完成（zhi_platform 库）"
  if command -v htpasswd >/dev/null 2>&1; then
    ADMIN_HASH=$(htpasswd -bnBC 10 "" "$ADMIN_PASSWORD" | tr -d ':\n')
    ADMIN_PW_SQL=$(printf '%s' "$ADMIN_PASSWORD" | sed "s/'/''/g")
    ADMIN_HASH_SQL=$(printf '%s' "$ADMIN_HASH" | sed "s/'/''/g")
    mysql -uroot -p"$MYSQL_PASSWORD" zhi_platform \
      -e "UPDATE sys_user SET password='$ADMIN_HASH_SQL', role='ADMIN' WHERE username='admin';"
    log "管理员密码已按 ADMIN_PASSWORD 初始化（admin / $ADMIN_PW_SQL，BCrypt 加密存储）"
  else
    warn "未安装 htpasswd（apache2-utils），管理员密码未重置，请使用 schema.sql 中的默认值"
  fi
}
setup_mysql

# ---------- 7. Neo4j 初始化 ----------
setup_neo4j() {
  command -v neo4j >/dev/null 2>&1 || die "Neo4j 未安装"
  systemctl stop neo4j 2>/dev/null || true
  if command -v neo4j-admin >/dev/null 2>&1; then
    if sudo -u neo4j neo4j-admin dbms set-initial-password "$NEO4J_PASSWORD" >/dev/null 2>&1; then
      log "Neo4j 初始密码已设置"
    fi
  fi
  systemctl start neo4j 2>/dev/null || service neo4j start 2>/dev/null || true
  for i in $(seq 1 20); do
    if cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "RETURN 1" >/dev/null 2>&1; then break; fi
    sleep 1
  done
  if cypher-shell -u neo4j -p neo4j "RETURN 1" >/dev/null 2>&1; then
    cypher-shell -u neo4j -p neo4j "ALTER CURRENT USER SET PASSWORD FROM 'neo4j' TO '$NEO4J_PASSWORD';" >/dev/null 2>&1 || true
    log "已将 Neo4j 默认密码改为新密码"
  fi
  cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "RETURN 1" >/dev/null 2>&1 || die "Neo4j 密码验证失败"
  CNT=$(cypher-shell -u neo4j -p "$NEO4J_PASSWORD" "MATCH (n) RETURN count(n)" 2>/dev/null | grep -Eo '^[0-9]+' | head -1)
  if [ "${CNT:-0}" = "0" ]; then
    cypher-shell -u neo4j -p "$NEO4J_PASSWORD" < "$NEO4J_INIT"
    log "Neo4j 示例数据初始化完成"
  else
    log "Neo4j 已有数据（$CNT 个节点），跳过初始化"
  fi
}
setup_neo4j

# ---------- 8. 拷贝产物 + systemd 服务 ----------
mkdir -p "$APP_DIR/backend" "$APP_DIR/frontend"
cp -f "$JAR" "$APP_DIR/backend/"
if [ -d "$SRC/frontend/dist" ]; then
  cp -rf "$SRC/frontend/dist/." "$APP_DIR/frontend/"
else
  cp -rf "$SRC/frontend/." "$APP_DIR/frontend/"
fi
cp -f "$SCHEMA" "$APP_DIR/backend/" 2>/dev/null || true
cp -f "$NEO4J_INIT" "$APP_DIR/backend/" 2>/dev/null || true

cat > /etc/systemd/system/zhi-platform.service <<EOF
[Unit]
Description=ZhiPlatform Backend (知联汇学)
After=network.target

[Service]
Type=simple
WorkingDirectory=$APP_DIR/backend
ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -jar $APP_DIR/backend/zhi-platform-1.0.0.jar
EnvironmentFile=$ENV_FILE
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable zhi-platform
systemctl restart zhi-platform
log "后端 systemd 服务已启动"

# ---------- 9. Nginx 配置 ----------
SERVER_NAME="${SERVER_NAME:-}"
if [ -z "$SERVER_NAME" ]; then
  SERVER_NAME=$(hostname -I 2>/dev/null | awk '{print $1}')
  SERVER_NAME="${SERVER_NAME:-localhost}"
fi
cat > /etc/nginx/conf.d/zhi-platform.conf <<EOF
server {
    listen 80;
    server_name $SERVER_NAME;

    root $APP_DIR/frontend;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        client_max_body_size 50m;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    gzip on;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;
    gzip_min_length 1k;
}
EOF
nginx -t >/dev/null 2>&1 && { systemctl reload nginx 2>/dev/null || nginx -s reload 2>/dev/null || true; } || warn "nginx 配置检查未通过，请手动检查 /etc/nginx/conf.d/zhi-platform.conf"
log "Nginx 已配置（80 端口，server_name=$SERVER_NAME）"

# ---------- 10. 等待后端就绪 + 汇总 ----------
info "等待后端启动..."
READY=0
for i in $(seq 1 30); do
  if curl -s -o /dev/null -m 2 -X POST http://127.0.0.1:8080/api/user/login -H 'Content-Type: application/json' -d '{"username":"x","password":"x"}'; then
    READY=1; break
  fi
  sleep 2
done
if [ "$READY" = "1" ]; then log "后端已就绪（8080）"; else warn "后端尚未就绪，请查看日志：journalctl -u zhi-platform -n 50"; fi

echo ""
echo "======================================================"
echo "  知联汇学部署完成！"
echo "  访问地址 : http://${SERVER_NAME}/"
echo "  管理员   : admin / ${ADMIN_PASSWORD}（首次登录后请立即修改密码）"
echo "  配置文件 : $ENV_FILE"
echo "  应用目录 : $APP_DIR"
echo "  常用命令 : systemctl restart zhi-platform"
echo "            journalctl -u zhi-platform -f"
echo "======================================================"