import request from './request'

// ==================== 班级 ====================

export function getClasses() {
  return request.get('/teaching/manage/clazz/list')
}

export function addClass(data) {
  return request.post('/teaching/manage/clazz', data)
}

export function updateClass(data) {
  return request.put('/teaching/manage/clazz', data)
}

export function deleteClass(id) {
  return request.delete(`/teaching/manage/clazz/${id}`)
}

export function getClassStudents(clazzId) {
  return request.get(`/teaching/manage/clazz/${clazzId}/students`)
}

export function addClassMember(clazzId, userId) {
  return request.post(`/teaching/manage/clazz/${clazzId}/member`, { userId })
}

export function removeClassMember(clazzId, userId) {
  return request.delete(`/teaching/manage/clazz/${clazzId}/member/${userId}`)
}

// ==================== 小组 ====================

export function getGroups(clazzId) {
  return request.get('/teaching/manage/group/list', { params: { clazzId } })
}

export function addGroup(data) {
  return request.post('/teaching/manage/group', data)
}

export function updateGroup(data) {
  return request.put('/teaching/manage/group', data)
}

export function deleteGroup(id) {
  return request.delete(`/teaching/manage/group/${id}`)
}

export function getGroupMembers(groupId) {
  return request.get(`/teaching/manage/group/${groupId}/members`)
}

export function addGroupMember(groupId, userId) {
  return request.post(`/teaching/manage/group/${groupId}/member`, { userId })
}

export function removeGroupMember(groupId, userId) {
  return request.delete(`/teaching/manage/group/${groupId}/member/${userId}`)
}

// ==================== 学生候选 ====================

export function getAllStudents() {
  return request.get('/teaching/manage/students')
}

export function getStudentCandidates(clazzId) {
  return request.get('/teaching/manage/students/candidates', { params: { clazzId } })
}

// ==================== 布置任务（教师） ====================

export function getAssignments() {
  return request.get('/teaching/manage/assignment/list')
}

export function createAssignment(data) {
  return request.post('/teaching/manage/assignment', data)
}

export function deleteAssignment(id) {
  return request.delete(`/teaching/manage/assignment/${id}`)
}

export function getAssignmentRecords(assignmentId) {
  return request.get(`/teaching/manage/assignment/${assignmentId}/records`)
}

export function gradeRecord(assignmentId, userId, score) {
  return request.put(`/teaching/manage/assignment/${assignmentId}/grade`, { userId, score })
}

// ==================== 学习资料（教师） ====================

export function getMaterials(clazzId) {
  return request.get('/teaching/manage/material/list', { params: { clazzId } })
}

export function addMaterial(data) {
  return request.post('/teaching/manage/material', data)
}

export function updateMaterial(data) {
  return request.put('/teaching/manage/material', data)
}

export function deleteMaterial(id) {
  return request.delete(`/teaching/manage/material/${id}`)
}

// ==================== 学习资料（学生） ====================

export function getMyMaterials(userId) {
  return request.get('/teaching/material/my', { params: { userId } })
}

// ==================== 学生任务 ====================

export function getMyTasks() {
  return request.get('/teaching/task/list')
}

export function submitTask(assignmentId, answer) {
  return request.post('/teaching/task/submit', { assignmentId, answer })
}