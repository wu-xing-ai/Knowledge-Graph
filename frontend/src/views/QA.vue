<template>
  <div class="page-container qa-page">
    <div class="qa-layout">
      <!-- 左侧问答区 -->
      <div class="chat-panel glass-panel">
        <div class="panel-header">
          <h3>智能问答</h3>
          <el-radio-group v-model="qaMode" size="small">
            <el-radio-button label="kbqa">KBQA</el-radio-button>
            <el-radio-button label="graphrag">GraphRAG</el-radio-button>
          </el-radio-group>
          <div style="font-size:12px;color:var(--text-secondary);margin-top:4px;">
            KBQA：本地知识库快速检索；GraphRAG：AI 大模型增强回答（较慢）。KBQA 检索不到时也会自动用 AI 回答。
          </div>
        </div>

        <!-- 预设问句 -->
        <div v-if="messages.length === 0" class="templates">
          <p style="color:var(--text-secondary);margin-bottom:12px;">常见跨学科问题：</p>
          <el-button v-for="t in templates" :key="t.id" text @click="sendMessage(t.question)" class="template-btn">
            {{ t.question }}
          </el-button>
        </div>

        <!-- 消息列表 -->
        <div class="messages" ref="messagesRef">
          <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
            <div class="bubble">
              <el-tag v-if="msg.mode === 'graphrag' || msg.mode === 'kbqa_ai'" size="small" type="warning" effect="plain" style="margin-bottom:4px;">AI 回答</el-tag>
              <div v-html="formatMessage(msg.content)" class="msg-content"></div>
            </div>
          </div>
          <div v-if="loading" class="msg assistant">
            <div class="bubble loading">
              <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
            </div>
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
      <div class="graph-panel glass-panel">
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
import { saveRecord } from '@/api/exercise'
import { useUserStore } from '@/store/user'
import { getGraphData, expandNode } from '@/api/graph'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'

const userStore = useUserStore()
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
      messages.value.push({ role: 'assistant', content: data.answer, mode: data.mode })
      highlightIds.value = data.relatedNodeIds || []
      saveQaRecord(q)

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

  // 保存问答历史（不影响主流程）
  async function saveQaRecord(question) {
    try {
      const userId = userStore.user?.userId
      if (!userId) return
      await saveRecord({ userId, actionType: 'QA', targetType: qaMode.value, extra: question })
    } catch (e) {
      // 历史保存失败不影响问答
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
  gap: 0;
}
.glass-panel {
  background: var(--bg-panel);
}
.chat-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border);
}
.graph-panel {
  width: 50%;
  display: flex;
  flex-direction: column;
}
.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-panel);

  h3 { font-size: 16px; color: var(--text-primary); }
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
  color: var(--text-secondary);
  &:hover { color: var(--primary); }
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-page);
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
  border-radius: 10px;
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
  background: var(--bg-panel);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}
.bubble.loading {
  display: flex;
  gap: 6px;
  align-items: center;
}
.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  animation: bounce 1.2s ease-in-out infinite;
  &:nth-child(2) { animation-delay: 0.15s; }
  &:nth-child(3) { animation-delay: 0.3s; }
}
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-5px); opacity: 1; }
}
.input-area {
  padding: 12px 16px;
  background: var(--bg-panel);
  border-top: 1px solid var(--border);
}
</style>
