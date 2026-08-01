<template>
  <div class="page-container graph-page">
    <div class="page-header">
      <h2>交叉学科知识图谱</h2>
      <div style="display:flex;gap:8px;">
        <el-button @click="exportPNG">导出PNG</el-button>
        <el-button type="primary" @click="exportJSON">导出JSON</el-button>
      </div>
    </div>

    <!-- 筛选面板 -->
    <el-card shadow="never" class="filter-card">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center;">
        <el-select v-model="filters.subjectIds" multiple collapse-tags placeholder="选择学科" style="width:300px;" @change="fetchData">
          <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索知识点" style="width:200px;" clearable @keyup.enter="fetchData" />
        <el-select v-model="layoutType" placeholder="布局" style="width:130px;">
          <el-option label="层次" value="dagre" />
          <el-option label="力导向" value="force" />
          <el-option label="环形" value="circular" />
          <el-option label="径向" value="radial" />
        </el-select>
        <el-button type="primary" @click="fetchData">
          <el-icon><Search /></el-icon>筛选
        </el-button>
      </div>
    </el-card>

    <!-- 图谱 -->
    <KnowledgeGraph
      :data="graphData"
      :highlight-node-ids="highlightIds"
      :layout="layoutType"
      height="calc(100vh - 260px)"
      style="min-height: 540px"
      @node-dblclick="onNodeDblclick"
    />

    <!-- 节点详情 -->
    <NodeDetail v-model="detailVisible" :node="selectedNode" @expand="onExpand" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getGraphData, expandNode } from '@/api/graph'
import { getSubjects } from '@/api/knowledge'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'
import NodeDetail from '@/components/NodeDetail.vue'
import { ElMessage } from 'element-plus'

const subjects = ref([])
const graphData = ref({ nodes: [], edges: [] })
const highlightIds = ref([])
const layoutType = ref('dagre')
const detailVisible = ref(false)
const selectedNode = ref(null)

const filters = reactive({ subjectIds: [], keyword: '' })

async function fetchData() {
  try {
    const res = await getGraphData({
      subjectIds: filters.subjectIds.length ? filters.subjectIds.join(',') : undefined,
      keyword: filters.keyword || undefined,
      limit: 200
    })
    if (res.code === 200) {
      graphData.value = res.data
      highlightIds.value = []
    }
  } catch (e) {
    // handled
  }
}

function onNodeDblclick(node) {
  selectedNode.value = node
  detailVisible.value = true
}

async function onExpand(node) {
  try {
    const res = await expandNode(node.id)
    if (res.code === 200 && res.data.nodes.length > 0) {
      const existingIds = new Set(graphData.value.nodes.map(n => n.id))
      const newNodes = res.data.nodes.filter(n => !existingIds.has(n.id))
      graphData.value.nodes.push(...newNodes)
      graphData.value.edges.push(...res.data.edges)
      highlightIds.value = [node.id, ...res.data.nodes.map(n => n.id)]
      detailVisible.value = false
    }
  } catch (e) {
    // handled
  }
}

function exportPNG() {
  ElMessage.info('请使用浏览器截图功能导出图谱')
}

function exportJSON() {
  const blob = new Blob([JSON.stringify(graphData.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'knowledge-graph.json'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  const res = await getSubjects()
  if (res.code === 200) subjects.value = res.data
  fetchData()
})
</script>

<style scoped lang="scss">
.graph-page {
  position: relative;
  z-index: 1;
}
.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-panel);
}
</style>
