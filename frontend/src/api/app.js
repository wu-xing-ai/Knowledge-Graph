import request from './request'

/**
 * 应用运行状态：前端启动时调用一次，用于判断是否处于演示模式。
 *
 * 正常模式（AUTH_ENABLED=true）：返回 { authRequired: true }
 * 演示模式（AUTH_ENABLED=false）：额外附带回演示账号登录信息
 *   { authRequired: false, token, role, username, realName, userId }
 */
export function getAppStatus() {
  return request.get('/app/status')
}
