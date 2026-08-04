#!/usr/bin/env bash
# ============================================================================
# 知联汇学 Docker 一键部署脚本
# 依赖：已安装 Docker + docker compose 插件（Docker 20.10+）
#
# 用法：
#   sudo bash scripts/docker-deploy.sh up       构建并启动（默认）
#   sudo bash scripts/docker-deploy.sh down     停止并删除容器
#   sudo bash scripts/docker-deploy.sh restart  重建并重启
#   sudo bash scripts/docker-deploy.sh logs     查看后端日志
#   sudo bash scripts/docker-deploy.sh status   查看容器状态
#
# 配置：自动读取仓库根目录 .env（MYSQL_PASSWORD / NEO4J_PASSWORD / JWT_SECRET /
#       MODELSCOPE_API_KEY / PADDLE_OCR_TOKEN），没有则自动生成随机密码。
# ============================================================================
set -euo pipefail

C_G='\033[32m'; C_C='\033[36m'; C_Y='\033[33m'; C_R='\033[31m'; C_0='\033[0m'
log()  { echo -e "${C_G}[OK]${C_0} $*"; }
info() { echo -e "${C_C}[..]${C_0} $*"; }
warn() { echo -e "${C_Y}[!]${C_0} $*"; }
die()  { echo -e "${C_R}[错误]${C_0} $*" >&2; exit 1; }

cd "$(dirname "$0")/.."

# ---------- 0. 检查 Docker ----------
command -v docker >/dev/null 2>&1 || die "未安装 Docker，请先安装（Ubuntu: sudo apt install -y docker.io docker-compose-plugin）"
docker compose version >/dev/null 2>&1 || die "docker compose 插件不可用，请安装 docker-compose-plugin"

CMD="${1:-up}"

# ---------- 1. 加载配置 ----------
if [ -f .env ]; then
  info "加载仓库根目录 .env"
  # Windows 上传的 .env 可能是 CRLF，先转 LF 再加载，避免 $'\r' 报错
  _env_tmp="$(mktemp)"
  sed 's/\r$//' ./.env > "$_env_tmp"
  set -a; . "$_env_tmp"; set +a
  rm -f "$_env_tmp"
fi
MYSQL_PASSWORD="${MYSQL_PASSWORD:-${SPRING_DATASOURCE_PASSWORD:-}}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-${SPRING_NEO4J_AUTHENTICATION_PASSWORD:-}}"
JWT_SECRET="${JWT_SECRET:-}"
if [ -z "$MYSQL_PASSWORD" ]; then MYSQL_PASSWORD="$(openssl rand -base64 18 | tr -d '=+/')"; fi
if [ -z "$NEO4J_PASSWORD" ]; then NEO4J_PASSWORD="$(openssl rand -base64 18 | tr -d '=+/')"; fi
if [ -z "$JWT_SECRET" ]; then JWT_SECRET="$(openssl rand -base64 48 | tr -d '=+/')"; fi
export MYSQL_PASSWORD NEO4J_PASSWORD JWT_SECRET MODELSCOPE_API_KEY PADDLE_OCR_TOKEN

case "$CMD" in
  down)
    docker compose down
    log "已停止并删除容器（数据卷保留）"
    ;;
  logs)
    docker compose logs -f --tail=100 backend
    ;;
  status)
    docker compose ps
    ;;
  restart|up)
    info "MYSQL_PASSWORD=$MYSQL_PASSWORD"
    info "NEO4J_PASSWORD=$NEO4J_PASSWORD"
    info "构建并启动容器（首次较慢，需下载镜像+构建）..."
    docker compose up -d --build

    info "等待后端就绪..."
    for i in $(seq 1 90); do
      if curl -s -o /dev/null -m 2 -X POST http://127.0.0.1:8080/api/user/login \
           -H 'Content-Type: application/json' -d '{"username":"x","password":"x"}'; then
        break
      fi
      sleep 2
    done

    # 初始化 Neo4j 示例数据（仅当图为空）
    CNT=$(docker compose exec -T neo4j cypher-shell -u neo4j -p "$NEO4J_PASSWORD" \
          "MATCH (n) RETURN count(n)" 2>/dev/null | grep -Eo '^[0-9]+' | head -1 || echo 0)
    if [ "${CNT:-0}" = "0" ]; then
      info "Neo4j 为空，导入示例数据..."
      docker compose exec -T neo4j cypher-shell -u neo4j -p "$NEO4J_PASSWORD" \
        < backend/src/main/resources/neo4j-init.cypher
      log "Neo4j 示例数据初始化完成"
    else
      log "Neo4j 已有数据（$CNT 个节点），跳过初始化"
    fi

    echo ""
    echo "======================================================"
    echo "  Docker 部署完成！"
    echo "  访问地址 : http://服务器IP/"
    echo "  管理员   : admin / 123456（首次登录后请立即修改密码）"
    echo "  常用命令 : bash scripts/docker-deploy.sh status|logs|down|restart"
    echo "  数据卷   : docker volume ls（mysql-data / neo4j-data，down 不会删除）"
    echo "======================================================"
    ;;
  *)
    die "未知命令：$CMD（支持 up/down/restart/logs/status）"
    ;;
esac