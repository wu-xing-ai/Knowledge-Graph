import request from './request'

export function getGraphData(params) {
  return request.get('/graph/data', { params })
}

export function expandNode(nodeId) {
  return request.get('/graph/expand', { params: { nodeId } })
}

export function saveGraphConfig(data) {
  return request.post('/graph/config/save', data)
}

export function getGraphConfigs() {
  return request.get('/graph/config/list')
}

export function deleteGraphConfig(id) {
  return request.delete(`/graph/config/${id}`)
}
