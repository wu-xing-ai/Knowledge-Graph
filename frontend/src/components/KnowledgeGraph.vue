<template>
  <div ref="container" class="knowledge-graph" :style="{ height }">
    <div class="graph-legend">
      <span v-for="s in legendItems" :key="s.label" class="legend-item">
        <i
          class="swatch"
          :class="{ ring: s.cross }"
          :style="s.cross ? { borderColor: s.color } : { background: s.color }"
        ></i>{{ s.label }}
      </span>
    </div>
    <!-- 节点悬浮提示：应用场景与衔接逻辑（功能.md 2.1） -->
    <div
      v-if="tooltip.visible"
      class="node-tooltip"
      :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
    >
      <div class="tip-title">{{ tooltip.node?.fullLabel || tooltip.node?.label }}</div>
      <div class="tip-meta">{{ tooltip.node?.subject }}<template v-if="tooltip.node?.isCross"> · 跨学科</template></div>
      <div v-if="tooltip.node?.description" class="tip-desc">{{ tooltip.node.description }}</div>
      <div v-if="tooltip.node?.examOutline" class="tip-outline">考纲要求：{{ tooltip.node.examOutline }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, onBeforeUnmount, nextTick, computed } from 'vue'
import G6 from '@antv/g6'

const props = defineProps({
  data: { type: Object, default: () => ({ nodes: [], edges: [] }) },
  height: { type: String, default: '500px' },
  highlightNodeIds: { type: Array, default: () => [] },
  layout: { type: String, default: 'dagre' }
})

const emit = defineEmits(['nodeClick', 'nodeDblclick'])

const container = ref(null)
let graph = null
// 节点悬浮提示状态
const tooltip = reactive({ visible: false, x: 0, y: 0, node: null })

// 边标签顶层分组：把标签文本从边分组搬到节点之上，避免与节点重叠时被圆球盖住
let edgeLabelGroup = null

const LEGEND = [
  { label: '高中物理', color: '#5B9BD5' },
  { label: '高中化学', color: '#ED7D31' },
  { label: '高中生物', color: '#70AD47' },
  { label: '航天拓展', color: '#8859A3' },
  { label: '环境科普', color: '#39B5A8' },
  { label: '水污染治理', color: '#39B5A8' },
  { label: '跨学科', color: '#6055A5', cross: true }
]

// 学科底色映射（后端未返回颜色时的兜底）
const SUBJECT_COLORS = {
  '高中物理': '#5B9BD5',
  '高中化学': '#ED7D31',
  '高中生物': '#70AD47',
  '航天拓展': '#8859A3',
  '环境科普': '#39B5A8',
  '水污染治理': '#39B5A8'
}
// 莫兰迪磨砂粘土质感（Claymorphism）：低饱和、无刺眼高光，柔和边缘阴影
// 径向渐变 r(fx, fy, fr)：0 浅色 → 0.7 主色 → 1 深色（去掉了 0:#FFFFFF 亮白高光）
const SUBJECT_GRADIENTS = {
  '高中物理': { fill: 'r(0.3, 0.3, 0.9) 0:#7EB2E6 0.7:#5B9BD5 1:#4178AC', glow: 'rgba(91, 155, 213, 0.25)' },
  '高中化学': { fill: 'r(0.3, 0.3, 0.9) 0:#F49B5D 0.7:#ED7D31 1:#C55A11', glow: 'rgba(237, 125, 49, 0.25)' },
  '高中生物': { fill: 'r(0.3, 0.3, 0.9) 0:#8EC268 0.7:#70AD47 1:#548235', glow: 'rgba(112, 173, 71, 0.25)' },
  '航天拓展': { fill: 'r(0.3, 0.3, 0.9) 0:#A57ABF 0.7:#8859A3 1:#613878', glow: 'rgba(136, 89, 163, 0.25)' },
  '环境科普': { fill: 'r(0.3, 0.3, 0.9) 0:#5CCBC0 0.7:#39B5A8 1:#258379', glow: 'rgba(57, 181, 168, 0.25)' },
  '水污染治理': { fill: 'r(0.3, 0.3, 0.9) 0:#5CCBC0 0.7:#39B5A8 1:#258379', glow: 'rgba(57, 181, 168, 0.25)' }
}
// 跨学科标识 = 墨紫描边（#6055A5 细紫圈）；球体保持学科莫兰迪磨砂渐变
const CROSS_STROKE = { color: '#6055A5', width: 2 }
const DEFAULT_GLOW = 'rgba(0, 0, 0, 0.15)'
const DEFAULT_NODE_COLOR = '#8F959E'

const legendItems = computed(() => {
  const subjects = new Set((props.data.nodes || []).map(n => n.subject).filter(Boolean))
  const filtered = LEGEND.filter(l => subjects.has(l.label))
  const hasCross = (props.data.nodes || []).some(n => n.isCross)
  if (hasCross) { const cross = LEGEND.find(l => l.cross); if (cross) filtered.push(cross) }
  return filtered
})

/* ---------- 图初始化 ---------- */
function initGraph() {
  if (!container.value) return
  const width = container.value.offsetWidth
  const height = container.value.offsetHeight

  graph = new G6.Graph({
    container: container.value,
    width,
    height,
    fitView: true,
    fitViewPadding: 16,
    // 关闭布局动画：200+ 节点下动画过渡是主要卡顿来源，渲染直接到位
    animate: false,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node']
    },
    layout: getLayoutConfig(),
    defaultNode: {
      type: 'ellipse',
      size: [118, 48],
      style: {
        lineWidth: 1.5,
        stroke: '#FFFFFF',
        cursor: 'pointer'
      },
      labelCfg: {
        style: {
          fill: '#FFFFFF',
          fontSize: 18,
          fontWeight: 600
        }
      }
    },
    defaultEdge: {
      type: 'quadratic',
      style: {
        lineWidth: 1.5,
        stroke: '#C4C9D0',
        endArrow: {
          path: G6.Arrow.triangle(7, 9, 0),
          fill: '#C4C9D0',
          d: 0
        }
      },
      labelCfg: {
        // 水平排布 + 白底圆角衬底：配合 raiseEdgeLabels 置顶，密集区也不会被节点/连线/其他标签挡住
        autoRotate: false,
        style: {
          fill: '#646A73',
          fontSize: 12,
          fontWeight: 500,
          background: {
            fill: '#FFFFFF',
            fillOpacity: 0.88,
            radius: 4,
            padding: [2, 5, 2, 5]
          }
        }
      }
    },
    nodeStateStyles: {
      highlight: {
        style: {
          stroke: '#1783FF',
          lineWidth: 3
        }
      },
      dim: {
        style: {
          opacity: 0.25
        }
      }
    },
    edgeStateStyles: {
      highlight: {
        style: {
          stroke: '#1783FF',
          lineWidth: 2.5
        }
      },
      dim: {
        style: {
          opacity: 0.1
        }
      }
    }
  })

  graph.on('node:click', (e) => {
    emit('nodeClick', e.item.getModel())
  })

  graph.on('node:dblclick', (e) => {
    emit('nodeDblclick', e.item.getModel())
  })

  // 悬浮显示节点详情（位置用画布坐标，随缩放平移天然正确；拖动画布/节点时隐藏）
  graph.on('node:mouseenter', (e) => {
    tooltip.node = e.item.getModel()
    tooltip.x = Math.min(e.canvasX + 14, width - 280)
    tooltip.y = e.canvasY + 16
    tooltip.visible = true
  })
  const hideTooltip = () => { tooltip.visible = false }
  graph.on('node:mouseleave', hideTooltip)
  graph.on('canvas:dragstart', hideTooltip)
  graph.on('node:dragstart', hideTooltip)
  graph.on('wheelzoom', hideTooltip)

  updateGraph()
}

function getLayoutConfig() {
  switch (props.layout) {
    case 'circular':
      return { type: 'circular', preventOverlap: true, nodeSpacing: 30, radius: 260 }
    case 'radial':
      return { type: 'radial', preventOverlap: true, nodeSpacing: 30, unitRadius: 160, linkDistance: 220 }
    case 'dagre':
      // 层次布局：dagre 内置交叉最小化（barycenter），TB 方向在本页面画布下缩放最优
      return { type: 'dagre', rankdir: 'TB', nodesep: 6, ranksep: 32 }
    default:
      return {
        type: 'force',
        preventOverlap: true,
        nodeStrength: -400,
        edgeStrength: 0.3,
        collideStrength: 1,
        nodeSpacing: 40,
        linkDistance: 220
      }
  }
}

function formatData(data) {
  const nodes = (data.nodes || []).map(n => {
    const color = n.color || SUBJECT_COLORS[n.subject] || DEFAULT_NODE_COLOR
    const isCross = !!n.isCross
    // 去掉后端返回的顶层 color 字段：G6 v4 会把它当作权威颜色（fill 和 stroke 都用它），
    // 导致 style.stroke（跨学科紫描边）被忽略，颜色只能通过 style 控制。
    const { color: _ignoredColor, ...nodeData } = n
    return {
      ...nodeData,
      id: String(n.id),
      label: n.name?.length > 8 ? n.name.substring(0, 8) + '...' : n.name,
      fullLabel: n.name,
      style: {
        fill: (SUBJECT_GRADIENTS[n.subject] || {}).fill || color,
        // 跨学科节点：墨紫描边细圈；单学科节点：白描边
        stroke: isCross ? CROSS_STROKE.color : '#FFFFFF',
        lineWidth: isCross ? CROSS_STROKE.width : 1.5,
        shadowColor: (SUBJECT_GRADIENTS[n.subject] || {}).glow || DEFAULT_GLOW,
        shadowBlur: 6,
        shadowOffsetX: 0,
        shadowOffsetY: 2
      },
      size: isCross ? [146, 58] : [118, 48]
    }
  })

  const edges = (data.edges || []).map((e, i) => {
    return {
      ...e,
      id: `edge-${i}`,
      source: String(e.source),
      target: String(e.target),
      label: e.relationType || '',
      // 曲线弧度：按边索引交替左右弯（幅度 20，配合 dagre 层次对齐更干净）
      curveOffset: i % 2 === 0 ? 20 : -20
    }
  })

  return { nodes, edges }
}

// 边标签置顶：G6 v4 中标签随边绘制在节点下层。将每条边的 text-shape 搬到
// 根分组下的专用顶层分组（与节点/边同一坐标系，缩放平移自动跟随；add 会自动从原父级摘除）
function raiseEdgeLabels() {
  if (!graph) return
  if (!edgeLabelGroup || edgeLabelGroup.get('destroyed')) {
    edgeLabelGroup = graph.get('group').addGroup({ id: 'edge-label-top-group' })
  }
  edgeLabelGroup.toFront()
  graph.getEdges().forEach(edge => {
    const container = edge.getContainer()
    // 先搬底衬 rect（text-bg-shape）再搬文字，保证文字压在底衬之上
    container.findAllByName('text-bg-shape').forEach(bg => edgeLabelGroup.add(bg))
    container.findAllByName('text-shape').forEach(label => edgeLabelGroup.add(label))
  })
}

function updateGraph() {
  if (!graph) return
  // 已搬到顶层分组的旧标签不会随旧边一起销毁，先清空避免残留重复
  if (edgeLabelGroup && !edgeLabelGroup.get('destroyed')) edgeLabelGroup.clear()
  const formatted = formatData(props.data)
  graph.data(formatted)
  graph.render()

  applyHighlight()
}

function applyHighlight() {
  if (!graph) return
  const highlightIds = props.highlightNodeIds.map(String)

  if (highlightIds.length === 0) {
    graph.getNodes().forEach(n => graph.clearItemStates(n))
    graph.getEdges().forEach(e => graph.clearItemStates(e))
    return
  }

  graph.getNodes().forEach(node => {
    const model = node.getModel()
    if (highlightIds.includes(model.id)) {
      graph.setItemState(node, 'highlight', true)
      graph.setItemState(node, 'dim', false)
    } else {
      graph.setItemState(node, 'dim', true)
      graph.setItemState(node, 'highlight', false)
    }
  })

  graph.getEdges().forEach(edge => {
    const model = edge.getModel()
    if (highlightIds.includes(String(model.source)) || highlightIds.includes(String(model.target))) {
      graph.setItemState(edge, 'highlight', true)
      graph.setItemState(edge, 'dim', false)
    } else {
      graph.setItemState(edge, 'dim', true)
      graph.setItemState(edge, 'highlight', false)
    }
  })
}

let dataTimer = null
watch(() => props.data, () => {
  // 防抖：展开节点等连续变更时合并为一次渲染
  if (dataTimer) clearTimeout(dataTimer)
  dataTimer = setTimeout(() => {
    dataTimer = null
    updateGraph()
  }, 200)
}, { deep: true })
watch(() => props.highlightNodeIds, applyHighlight, { deep: true })
watch(() => props.layout, () => {
  if (graph) {
    graph.updateLayout(getLayoutConfig())
  }
})

onMounted(() => {
  nextTick(initGraph)
  let resizeTimer = null
  window.addEventListener('resize', () => {
    if (resizeTimer) return
    resizeTimer = setTimeout(() => {
      resizeTimer = null
      if (graph && container.value) {
        graph.changeSize(container.value.offsetWidth, container.value.offsetHeight)
        graph.fitView()
      }
    }, 200)
  })
})

onBeforeUnmount(() => {
  if (graph) {
    graph.destroy()
    graph = null
    edgeLabelGroup = null
  }
})

// 导出 PNG：G6 画布为透明底 canvas，合成莫兰迪底色后导出，与页面观感一致
function exportPNG() {
  if (!graph) return null
  const canvasEl = graph.get('canvas').get('el')
  const out = document.createElement('canvas')
  out.width = canvasEl.width
  out.height = canvasEl.height
  const ctx = out.getContext('2d')
  ctx.fillStyle = '#F4F6F9'
  ctx.fillRect(0, 0, out.width, out.height)
  ctx.drawImage(canvasEl, 0, 0)
  return out.toDataURL('image/png')
}
defineExpose({ exportPNG })
</script>

<style scoped lang="scss">
.knowledge-graph {
  position: relative;
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border);
  /* 莫兰迪浅色画布：柔和护眼灰蓝 + 微弱网格点，增加空间层次感 */
  background-color: #F4F6F9;
  background-image: radial-gradient(circle, #E2E8F0 1px, transparent 1px);
  background-size: 24px 24px;
}

.graph-legend {
  position: absolute;
  top: 12px;
  right: 14px;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #FFFFFF;
  border: 1px solid var(--border);
  pointer-events: none;

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    color: var(--text-secondary);

    i {
      width: 9px;
      height: 9px;
      border-radius: 3px;

      &.ring {
        background: #FFFFFF;
        border: 2px solid;
        box-sizing: border-box;
      }
    }
  }
}

/* 节点悬浮提示卡片 */
.node-tooltip {
  position: absolute;
  z-index: 3;
  max-width: 260px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-pop);
  pointer-events: none;

  .tip-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 2px;
  }
  .tip-meta {
    font-size: 11px;
    color: var(--primary);
    margin-bottom: 6px;
  }
  .tip-desc {
    font-size: 12px;
    line-height: 1.6;
    color: var(--text-secondary);
  }
  .tip-outline {
    margin-top: 6px;
    font-size: 11px;
    color: var(--text-muted);
  }
}
</style>
