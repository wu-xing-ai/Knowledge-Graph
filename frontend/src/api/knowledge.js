import request from './request'

export function getSubjects(type) {
  return request.get('/knowledge/subject/list', { params: { type } })
}

export function addSubject(data) {
  return request.post('/knowledge/subject/add', data)
}

export function deleteSubject(id) {
  return request.delete(`/knowledge/subject/${id}`)
}

export function addKnowledge(data) {
  return request.post('/knowledge/add', data)
}

export function updateKnowledge(data) {
  return request.put('/knowledge/update', data)
}

export function deleteKnowledge(id) {
  return request.delete(`/knowledge/${id}`)
}

export function getKnowledge(id) {
  return request.get(`/knowledge/${id}`)
}

export function queryKnowledge(params) {
  return request.get('/knowledge/query', { params })
}

export function getCrossKnowledge() {
  return request.get('/knowledge/cross')
}

export function batchImport(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/batch-import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
