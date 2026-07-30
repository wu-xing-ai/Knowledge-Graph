import request from './request'

export function kbqa(question) {
  return request.post('/qa/kbqa', { question })
}

export function graphRag(question) {
  return request.post('/qa/graphrag', { question })
}

export function getTemplates() {
  return request.get('/qa/template/list')
}

export function addTemplate(data) {
  return request.post('/qa/template/add', data)
}
