<template>
  <div class="page-container graph-page">
    <div class="page-header">
      <h2>交叉学科知识图谱</h2>
      <div style="display:flex;gap:8px;">
        <el-button @click="shareLink"><el-icon><Link /></el-icon>分享</el-button>
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
      ref="graphRef"
      :data="graphData"
      :highlight-node-ids="highlightIds"
      :layout="layoutType"
      height="calc(100vh - 260px)"
      style="min-height: 540px"
      @node-dblclick="onNodeDblclick"
    />

    <!-- 节点详情 -->
    <NodeDetail v-model="detailVisible" :node="selectedNode" @expand="onExpand" @collect="onCollect" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGraphData, expandNode } from '@/api/graph'
import { getSubjects } from '@/api/knowledge'
import { addFavorite } from '@/api/exercise'
import { useUserStore } from '@/store/user'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'
import NodeDetail from '@/components/NodeDetail.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const subjects = ref([])
const graphData = ref({ nodes: [], edges: [] })
const highlightIds = ref([])
const layoutType = ref('dagre')
const detailVisible = ref(false)
const selectedNode = ref(null)
const graphRef = ref(null)

const filters = reactive({ subjectIds: [], keyword: '' })

watch(layoutType, syncQuery)

// 筛选与布局同步到 URL query，生成可分享的视图链接
function syncQuery() {
  router.replace({
    query: {
      ...(filters.subjectIds.length ? { subjects: filters.subjectIds.join(',') } : {}),
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      layout: layoutType.value
    }
  })
}

function shareLink() {
  syncQuery()
  navigator.clipboard.writeText(window.location.href)
    .then(() => ElMessage.success('分享链接已复制，打开即可还原当前视图'))
    .catch(() => ElMessage.warning('复制失败，请手动复制地址栏链接'))
}

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
      syncQuery()
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

// 收藏知识点
async function onCollect(node) {
  try {
    const res = await addFavorite({
      userId: userStore.user?.userId,
      knowledgeId: node.id,
      knowledgeName: node.name,
      category: node.subject
    })
    if (res.code === 200) {
      ElMessage.success('已收藏')
    } else {
      ElMessage.error(res.message || '收藏失败')
    }
  } catch (e) {
    ElMessage.error('收藏失败')
  }
}

function exportPNG() {
  const dataUrl = graphRef.value?.exportPNG()
  if (!dataUrl) {
    ElMessage.warning('图谱尚未渲染完成')
    return
  }
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = 'knowledge-graph.png'
  a.click()
  ElMessage.success('已导出 PNG')
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
  // 从分享链接还原视图（学科筛选 / 关键词 / 布局）
  const q = route.query
  if (q.subjects) {
    filters.subjectIds = String(q.subjects).split(',').map(Number).filter(Boolean)
  }
  if (q.keyword) filters.keyword = String(q.keyword)
  if (q.layout && ['dagre', 'force', 'circular', 'radial'].includes(String(q.layout))) {
    layoutType.value = String(q.layout)
  }
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
