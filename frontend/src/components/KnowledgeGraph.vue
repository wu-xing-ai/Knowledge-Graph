<template>
  <div ref="container" class="knowledge-graph" :style="{ height: height }"></div>
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount, nextTick } from 'vue'
import G6 from '@antv/g6'

const props = defineProps({
  data: { type: Object, default: () => ({ nodes: [], edges: [] }) },
  height: { type: String, default: '500px' },
  highlightNodeIds: { type: Array, default: () => [] },
  layout: { type: String, default: 'force' }
})

const emit = defineEmits(['nodeClick', 'nodeDblclick'])

const container = ref(null)
let graph = null

function initGraph() {
  if (!container.value) return
  const width = container.value.offsetWidth
  const height = container.value.offsetHeight

  graph = new G6.Graph({
    container: container.value,
    width,
    height,
    fitView: true,
    fitViewPadding: 20,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node']
    },
    layout: getLayoutConfig(),
    defaultNode: {
      type: 'ellipse',
      size: [120, 50],
      style: {
        fill: '#4A90D9',
        stroke: '#fff',
        lineWidth: 2,
        shadowColor: 'rgba(0,0,0,0.15)',
        shadowBlur: 8,
        cursor: 'pointer'
      },
      labelCfg: {
        style: {
          fill: '#fff',
          fontSize: 12,
          fontWeight: 600
        }
      }
    },
    defaultEdge: {
      type: 'polyline',
      style: {
        stroke: '#C0C4CC',
        lineWidth: 1.5,
        endArrow: {
          path: G6.Arrow.triangle(6, 8, 0),
          fill: '#C0C4CC'
        }
      },
      labelCfg: {
        style: {
          fill: '#909399',
          fontSize: 10
        },
        autoRotate: true
      }
    },
    nodeStateStyles: {
      highlight: {
        style: {
          lineWidth: 3,
          shadowColor: '#6C5CE7',
          shadowBlur: 16
        }
      },
      dim: {
        style: {
          opacity: 0.3
        }
      }
    },
    edgeStateStyles: {
      highlight: {
        style: {
          stroke: '#6C5CE7',
          lineWidth: 2.5
        }
      },
      dim: {
        style: {
          opacity: 0.15
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

  updateGraph()
}

function getLayoutConfig() {
  switch (props.layout) {
    case 'circular':
      return { type: 'circular', preventOverlap: true, nodeSpacing: 30 }
    case 'radial':
      return { type: 'radial', preventOverlap: true, nodeSpacing: 30 }
    case 'dagre':
      return { type: 'dagre', rankdir: 'LR', nodesep: 40, ranksep: 80 }
    default:
      return {
        type: 'force',
        preventOverlap: true,
        nodeStrength: -200,
        edgeStrength: 0.1,
        collideStrength: 0.8,
        nodeSpacing: 20
      }
  }
}

function formatData(data) {
  const nodes = (data.nodes || []).map(n => ({
    id: String(n.id),
    label: n.name?.length > 8 ? n.name.substring(0, 8) + '...' : n.name,
    fullLabel: n.name,
    style: {
      fill: n.color || '#4A90D9'
    },
    size: n.isCross ? [130, 55] : [110, 45],
    ...n
  }))

  const edges = (data.edges || []).map((e, i) => ({
    id: `edge-${i}`,
    source: String(e.source),
    target: String(e.target),
    label: e.relationType || '',
    ...e
  }))

  return { nodes, edges }
}

function updateGraph() {
  if (!graph) return
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

watch(() => props.data, updateGraph, { deep: true })
watch(() => props.highlightNodeIds, applyHighlight, { deep: true })
watch(() => props.layout, () => {
  if (graph) {
    graph.updateLayout(getLayoutConfig())
  }
})

onMounted(() => {
  nextTick(initGraph)
  window.addEventListener('resize', () => {
    if (graph && container.value) {
      graph.changeSize(container.value.offsetWidth, container.value.offsetHeight)
    }
  })
})

onBeforeUnmount(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
})
</script>

<style scoped>
.knowledge-graph {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}
</style>
