import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '@/utils/markdown'

describe('renderMarkdown', () => {
  const sample = `首先不是流式输出，然后火箭发射中的推力主要与以下物理公式相关：

---

## 一、核心公式

### 1. 牛顿第二定律
$$F = ma$$

### 2. 动量定理（推力的本质）
$$F = \\frac{\\Delta p}{\\Delta t} = \\frac{m_{gas}}{dt} \\cdot v_e$$

说明：
- $m_{gas}/dt$：单位时间内喷射出的气体质量（质量流率）
- $v_e$：燃气喷射速度

---

## 二、齐奥尔科夫斯基火箭方程
$$\\Delta v = v_e \\ln\\left(\\frac{m_0}{m_f}\\right)$$

## 三、其他相关公式

| 公式 | 用途 |
|------|------|
| $p = mv$ | 动量定义 |
| $E_k = \\frac{1}{2}mv^2$ | 动能计算 |
| $F = G\\frac{Mm}{r^2}$ | 地球引力（需克服的重力） |`

  it('渲染块级公式（$$...$$）为 KaTeX display', () => {
    const html = renderMarkdown(sample)
    expect(html).toContain('katex-display')
    expect(html).toContain('class="katex"')
  })

  it('渲染行内公式（$...$）为 KaTeX inline', () => {
    const html = renderMarkdown(sample)
    expect(html).toContain('katex')
  })

  it('渲染 Markdown 表格', () => {
    const html = renderMarkdown(sample)
    expect(html).toContain('<table>')
    expect(html).toContain('<th>')
    expect(html).toContain('<td>')
  })

  it('渲染标题与加粗', () => {
    const html = renderMarkdown(sample)
    expect(html).toContain('<h2>')
    expect(html).toContain('<h3>')
  })

  it('空内容返回空串', () => {
    expect(renderMarkdown('')).toBe('')
    expect(renderMarkdown(null)).toBe('')
  })
})
