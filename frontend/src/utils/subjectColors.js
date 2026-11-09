// 学科配色统一入口
// 与 global.scss :root 学科变量（--color-*）及 KnowledgeGraph 图例保持一致，
// 各页面通过 el-tag :color 等 JS 场景引用此处，避免多处硬编码不一致
export const SUBJECT_COLORS = {
  '高中物理': '#5B9BD5',
  '高中化学': '#ED7D31',
  '高中生物': '#70AD47',
  '航天拓展': '#8859A3',
  '环境科普': '#39B5A8',
  '水污染治理': '#39B5A8',
  '交叉知识点': '#6055A5'
}

export function getSubjectColor(name) {
  return SUBJECT_COLORS[name] || '#8F959E'
}
