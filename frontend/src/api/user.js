import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function getUserList() {
  return request.get('/user/list')
}

export function updateUserStatus(userId, status) {
  return request.put('/user/status', { userId, status })
}

export function changePassword(data) {
  return request.put('/user/password', data)
}

// 批量导入学生账号（Excel：用户名/姓名/初始密码/班级名）
export function batchImportUsers(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/batch-import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
