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

export function documentExtract(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/knowledge/document/extract', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000 // 仅提交任务，很快返回 jobId
  })
}

export function getExtractStatus(jobId) {
  return request.get(`/knowledge/document/extract/status/${jobId}`)
}

export function documentImport(data) {
  return request.post('/knowledge/document/import', data, { timeout: 60000 })
}

export function downloadTemplate() {
  return request.get('/knowledge/template/download', { responseType: 'blob' })
}

export function getDocumentImports() {
  return request.get('/knowledge/document/list')
}

export function deleteDocumentImport(id) {
  return request.delete(`/knowledge/document/${id}`)
}

export function getDocumentDrafts() {
  return request.get('/knowledge/document/drafts')
}

export function getDocumentDraft(id) {
  return request.get(`/knowledge/document/draft/${id}`)
}

export function deleteDocumentDraft(id) {
  return request.delete(`/knowledge/document/draft/${id}`)
}

