/**
 * Markdown + KaTeX 渲染工具
 * 用于问答回答、知识点描述等含 LaTeX 公式与 Markdown 表格/列表的富文本。
 * - 行内公式：$...$
 * - 块级公式：$$...$$
 * - Markdown：表格、列表、标题、加粗、引用等
 */
import MarkdownIt from 'markdown-it'
import katexPluginModule from '@vscode/markdown-it-katex'
// CJS/ESM 互操作兼容：Vite 下默认导入即函数，纯 Node ESM 下是 { default: fn }
const katexPlugin = (katexPluginModule && katexPluginModule.default) ? katexPluginModule.default : katexPluginModule
import 'katex/dist/katex.min.css'

const md = new MarkdownIt({
  html: false, // 转义原始 HTML，避免注入
  linkify: true,
  breaks: true // 单个换行渲染为 <br>，与原来 formatMessage 的行为一致
})

md.use(katexPlugin, {
  throwOnError: false,
  errorColor: '#e06c75'
})

export function renderMarkdown(text) {
  if (!text) return ''
  return md.render(String(text))
}

export default renderMarkdown
