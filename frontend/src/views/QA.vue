<template>
  <div class="page-container qa-page">
    <div class="qa-layout">
      <!-- 左侧问答区 -->
      <div class="chat-panel">
        <div class="panel-header">
          <h3>智能问答</h3>
          <el-radio-group v-model="qaMode" size="small">
            <el-radio-button label="kbqa">KBQA</el-radio-button>
            <el-radio-button label="graphrag">GraphRAG</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 预设问句 -->
        <div v-if="messages.length === 0" class="templates">
          <p style="color:#909399;margin-bottom:12px;">常见跨学科问题：</p>
          <el-button v-for="t in templates" :key="t.id" text @click="sendMessage(t.question)" class="template-btn">
            {{ t.question }}
          </el-button>
        </div>

        <!-- 消息列表 -->
        <div class="messages" ref="messagesRef">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
            <div class="bubble">
              <div v-html="formatMessage(msg.content)" class="msg-content"></div>
            </div>
          </div>
          <div v-if="loading" class="msg assistant">
            <div class="bubble loading">思考中...</div>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="input-area">
          <el-input
            v-model="input"
            placeholder="请输入跨学科问题..."
            @keyup.enter="sendMessage()"
            :disabled="loading"
            size="large"
          >
            <template #append>
              <el-button type="primary" @click="sendMessage()" :loading="loading">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 右侧图谱联动 -->
      <div class="graph-panel">
        <div class="panel-header">
          <h3>关联知识图谱</h3>
        </div>
        <KnowledgeGraph
          :data="linkedGraphData"
          :highlight-node-ids="highlightIds"
          height="calc(100vh - 220px)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { kbqa, graphRag, getTemplates } from '@/api/qa'
import { getGraphData, expandNode } from '@/api/graph'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'

const qaMode = ref('kbqa')
const input = ref('')
const loading = ref(false)
const messages = ref([])
const templates = ref([])
const messagesRef = ref(null)
const linkedGraphData = ref({ nodes: [], edges: [] })
const highlightIds = ref([])

async function sendMessage(text) {
  const q = text || input.value.trim()
  if (!q) return
  input.value = ''

  messages.value.push({ role: 'user', content: q })
  scrollBottom()
  loading.value = true

  try {
    const api = qaMode.value === 'graphrag' ? graphRag : kbqa
    const res = await api(q)
    if (res.code === 200) {
      const data = res.data
      messages.value.push({ role: 'assistant', content: data.answer })
      highlightIds.value = data.relatedNodeIds || []

      // 如果有关联节点，加载图谱
      if (highlightIds.value.length > 0) {
        await loadRelatedGraph(highlightIds.value)
      }
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，回答出现问题，请稍后重试。' })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

async function loadRelatedGraph(nodeIds) {
  try {
    const res = await getGraphData({ limit: 50 })
    if (res.code === 200) {
      linkedGraphData.value = res.data
    }
  } catch (e) {
    // handled
  }
}

function formatMessage(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
}

function scrollBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

onMounted(async () => {
  const res = await getTemplates()
  if (res.code === 200) templates.value = res.data
})
</script>

<style scoped lang="scss">
.qa-page {
  padding: 0;
}
.qa-layout {
  display: flex;
  height: calc(100vh - 56px);
}
.chat-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
}
.graph-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
}
.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;

  h3 { font-size: 16px; }
}
.templates {
  padding: 20px;
  display: flex;
  flex-direction: column;
}
.template-btn {
  text-align: left;
  justify-content: flex-start;
  margin-bottom: 4px;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}
.msg {
  margin-bottom: 12px;
  display: flex;
  &.user { justify-content: flex-end; }
  &.assistant { justify-content: flex-start; }
}
.bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.8;
  .msg-content { word-break: break-word; }
}
.msg.user .bubble {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg.assistant .bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.bubble.loading {
  color: #909399;
}
.input-area {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebeef5;
}
</style>
