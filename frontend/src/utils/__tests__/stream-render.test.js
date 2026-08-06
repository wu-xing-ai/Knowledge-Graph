import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '@/utils/markdown'

const deltas = [
  "根据知识库查询，为您找到",
  "以下相关信息：\n\n**微",
  "生物降解** [高中生物",
  "]\n微生物分解有机污染物",
  "的过程，广泛应用于环境治",
  "理\n\n**化学氧化法**",
  " [环境科普]\n利用氧化",
  "剂（如臭氧、氯气）分解水",
  "中有机物\n\n\n---\n*",
  "*跨学科关联提示**：您",
  "的问题涉及环境科普、高中",
  "化学等多个学科领域的知识",
  "。"
]

describe('renderMarkdown 流式增量渲染', () => {
  it('逐段追加渲染不抛异常', () => {
    let content = ''
    for (const d of deltas) {
      content += d
      let html
      expect(() => { html = renderMarkdown(content) }).not.toThrow()
      // 每次渲染后 HTML 非空
      expect(html.length).toBeGreaterThan(0)
    }
  })
})
