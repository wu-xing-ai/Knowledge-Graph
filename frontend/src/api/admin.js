import request from './request'

// 管理员：用户列表（含密码哈希）
export function getAdminUsers() {
  return request.get('/admin/users')
}

// 管理员：新增账号（教师/学生）
export function adminAddUser(data) {
  return request.post('/admin/user/add', data)
}

// 管理员：重置密码
export function adminResetPassword(userId, password) {
  return request.put('/admin/user/password', { userId, password })
}

// 管理员：启用/禁用
export function adminUpdateStatus(userId, status) {
  return request.put('/admin/user/status', { userId, status })
}

// 操作日志
export function getAdminLogs() {
  return request.get('/admin/logs')
}

// 管理员：修改用户身份（角色）
export function adminUpdateRole(userId, role) {
  return request.put('/admin/user/role', { userId, role })
}

// 管理员：删除账号（拒绝教师注册申请）
export function adminDeleteUser(id) {
  return request.delete(`/admin/user/${id}`)
}
