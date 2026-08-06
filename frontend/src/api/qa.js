import request from './request'

export function kbqa(question) {
  return request.post('/qa/kbqa', { question })
}

export function graphRag(question) {
  return request.post('/qa/graphrag', { question })
}

/**
 * SSE 流式问答通用实现（KBQA / GraphRAG 共用）。
 * 事件：meta（关联节点 + mode）→ delta* → done / error。
 * handlers: { onMeta, onDelta, onDone, onError }，返回可中断的 AbortController。
 */
function streamRequest(url, question, handlers = {}) {
  const { onMeta, onDelta, onDone, onError } = handlers
  const controller = new AbortController()
  const token = localStorage.getItem('token')

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ question }),
    signal: controller.signal
  })
    .then(async res => {
      if (!res.ok || !res.body) {
        throw new Error(`流式请求失败 (HTTP ${res.status})`)
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let eventName = ''
      const handleLine = line => {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (!data) return
          let obj
          try {
            obj = JSON.parse(data)
          } catch {
            return
          }
          if (eventName === 'meta') {
            onMeta && onMeta(obj)
          } else if (eventName === 'delta') {
            onDelta && onDelta(obj.content || '')
          } else if (eventName === 'done') {
            onDone && onDone()
          } else if (eventName === 'error') {
            onError && onError(obj.message || '未知错误')
          }
        }
      }
      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buffer.indexOf('\n')) >= 0) {
          const line = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 1)
          handleLine(line)
        }
      }
      if (buffer.trim()) handleLine(buffer)
    })
    .catch(err => {
      if (err.name === 'AbortError') return
      onError && onError(err.message || '网络错误')
    })

  return controller
}

/** GraphRAG 流式问答（SSE） */
export function graphRagStream(question, handlers = {}) {
  return streamRequest('/api/qa/graphrag/stream', question, handlers)
}

/** KBQA 流式问答（SSE）：本地检索命中 / AI 兜底都逐段返回 */
export function kbqaStream(question, handlers = {}) {
  return streamRequest('/api/qa/kbqa/stream', question, handlers)
}

export function getTemplates() {
  return request.get('/qa/template/list')
}

export function addTemplate(data) {
  return request.post('/qa/template/add', data)
}
