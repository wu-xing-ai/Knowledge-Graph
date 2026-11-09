import request from './request'

export function getGraphData(params) {
  return request.get('/graph/data', { params })
}

export function expandNode(nodeId) {
  return request.get('/graph/expand', { params: { nodeId } })
}

export function getTrace(nodeId, depth = 3) {
  return request.get('/graph/trace', { params: { nodeId, depth } })
}

export function generateGraph(keywords, limit = 20) {
  return request.get('/graph/generate', {
    params: { keywords, limit },
    paramsSerializer: {
      indexes: null
    }
  })
}

export function getRelations() {
  return request.get('/graph/relations')
}

export function addRelation(data) {
  return request.post('/graph/relation/add', data)
}

export function deleteRelation(sourceId, targetId, relationType) {
  return request.delete('/graph/relation', { params: { sourceId, targetId, relationType } })
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
