#!/usr/bin/env bash
# ============================================================================
# 知联汇学 - 项目控制脚本（重启 / 启动 / 停止 / 状态 / 日志）
# 无需构建环境（不依赖 JDK/Maven/Node），只管理系统服务。
#
# 用法：
#   sudo bash restart.sh restart   重启全部服务（默认）
#   sudo bash restart.sh start     启动全部服务
#   sudo bash restart.sh stop      停止全部服务
#   sudo bash restart.sh status    查看各服务状态
#   sudo bash restart.sh logs      查看后端最近日志
#   sudo bash restart.sh backend   只重启后端（改配置/换包后最快生效）
# ============================================================================
set -euo pipefail

C_G='\033[32m'; C_C='\033[36m'; C_Y='\033[33m'; C_R='\033[31m'; C_0='\033[0m'
log()  { echo -e "${C_G}[OK]${C_0} $*"; }
info() { echo -e "${C_C}[..]${C_0} $*"; }
warn() { echo -e "${C_Y}[!]${C_0} $*"; }
die()  { echo -e "${C_R}[错误]${C_0} $*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "请用 sudo 运行：sudo bash $0 [restart|start|stop|status|logs|backend]"

CMD="${1:-restart}"

# ---------- 工具函数 ----------
has_service() { systemctl list-unit-files 2>/dev/null | grep -q "^${1}.service"; }

svc_do() { # svc_do <动作> <服务名> [备用服务名]
  local action="$1" name="$2" alias="${3:-}"
  local target="$name"
  if ! has_service "$name"; then
    [ -n "$alias" ] && has_service "$alias" && target="$alias" || { warn "跳过 $name（未安装/未注册服务）"; return 0; }
  fi
  case "$action" in
    start)   systemctl start "$target"   ;;
    stop)    systemctl stop "$target"    ;;
    restart) systemctl restart "$target" ;;
  esac
  log "$target ${action} 完成"
}

wait_backend() {
  info "等待后端就绪..."
  for i in $(seq 1 30); do
    if curl -s -o /dev/null -m 2 -X POST http://127.0.0.1:8080/api/user/login \
         -H 'Content-Type: application/json' -d '{"username":"x","password":"x"}'; then
      return 0
    fi
    sleep 2
  done
  return 1
}

print_status() {
  echo ""
  echo "=============================================="
  for s in mysql mariadb neo4j nginx zhi-model-service zhi-platform; do
    if has_service "$s"; then
      printf "  %-14s : %s\n" "$s" "$(systemctl is-active "$s" 2>/dev/null || echo unknown)"
    fi
  done
  echo "=============================================="
}

# ---------- 主逻辑 ----------
case "$CMD" in
  start)
    svc_do start mysql mariadb
    svc_do start neo4j
    svc_do start nginx
    has_service zhi-model-service && svc_do start zhi-model-service
    has_service zhi-platform && svc_do start zhi-platform || die "未部署后端，请先运行 install.sh"
    wait_backend && log "后端已就绪" || warn "后端未就绪，用 restart.sh logs 查看日志"
    print_status
    ;;
  stop)
    has_service zhi-platform && svc_do stop zhi-platform
    has_service zhi-model-service && svc_do stop zhi-model-service
    svc_do stop nginx
    svc_do stop neo4j
    svc_do stop mysql mariadb
    log "全部已停止"
    ;;
  restart)
    svc_do restart mysql mariadb
    svc_do restart neo4j
    svc_do restart nginx
    has_service zhi-model-service && svc_do restart zhi-model-service
    has_service zhi-platform && svc_do restart zhi-platform || die "未部署后端，请先运行 install.sh"
    if wait_backend; then log "后端已就绪，访问 http://服务器IP/"; else warn "后端未就绪，最近日志："; journalctl -u zhi-platform -n 30 --no-pager 2>/dev/null | tail -30 || true; fi
    print_status
    ;;
  backend)
    has_service zhi-platform || die "未部署后端，请先运行 install.sh"
    svc_do restart zhi-platform
    if wait_backend; then log "后端已就绪"; else warn "后端未就绪，最近日志："; journalctl -u zhi-platform -n 30 --no-pager 2>/dev/null | tail -30 || true; fi
    ;;
  backend)
    has_service zhi-platform || die "未部署后端，请先运行 install.sh"
    svc_do restart zhi-platform
    if wait_backend; then log "后端已就绪"; else warn "后端未就绪，最近日志："; journalctl -u zhi-platform -n 30 --no-pager 2>/dev/null | tail -30 || true; fi
    ;;
  status)
    print_status
    ;;
  logs)
    journalctl -u zhi-platform -n 50 --no-pager 2>/dev/null | tail -50 || echo "无后端日志"
    ;;
  *)
    die "未知命令：$CMD（支持 restart/start/stop/status/logs/backend）"
    ;;
esac