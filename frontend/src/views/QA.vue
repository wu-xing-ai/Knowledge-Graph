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
            <div class="bubble" :class="{ loading: msg.streaming && !msg.content }">
              <el-tag v-if="msg.mode === 'graphrag' || msg.mode === 'kbqa_ai'" size="small" type="warning" effect="plain" style="margin-bottom:4px;">AI 回答</el-tag>
              <!-- 等待首个 token：打字点 + 状态文字（检索/思考中） -->
              <div v-if="msg.streaming && !msg.content" class="typing-row">
                <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
                <span class="waiting-text">{{ msg.waitingText || '正在思考…' }}</span>
              </div>
              <!-- 开始输出：内容 + 闪烁光标 -->
              <template v-else>
                <div v-html="formatMessage(msg.content)" class="msg-content"></div>
                <span v-if="msg.streaming" class="stream-cursor"></span>
              </template>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { kbqaStream, graphRagStream, getTemplates } from '@/api/qa'
import { saveRecord } from '@/api/exercise'
import { useUserStore } from '@/store/user'
import { getGraphData, expandNode } from '@/api/graph'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'
import { renderMarkdown } from '@/utils/markdown'

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

  // KBQA 与 GraphRAG 都走 SSE 流式，前端逐字渲染
  await sendStream(q, qaMode.value)
}

// 统一流式回答：KBQA（本地检索 / AI 兜底）与 GraphRAG 都逐 token 渲染 + 闪烁光标
async function sendStream(q, mode) {
  loading.value = true
  // 必须用 reactive：直接改普通对象属性不会触发 Vue 响应式刷新，界面会“卡住不动”
  const assistantMsg = reactive({
    role: 'assistant',
    content: '',
    streaming: true,
    mode: mode,
    waitingText: mode === 'graphrag' ? 'AI 正在思考…' : '正在检索知识库…'
  })
  messages.value.push(assistantMsg)
  scrollBottom()

  const streamApi = mode === 'graphrag' ? graphRagStream : kbqaStream
  try {
    streamApi(q, {
      onMeta: async (meta) => {
        assistantMsg.content = ''
        // meta.mode 用于区分 KBQA 是否走了 AI 兜底（kbqa_ai 显示“AI 回答”标签）
        if (meta.mode) {
          assistantMsg.mode = meta.mode
          // 本地检索未命中，后端升级为 AI 回答：把状态文字切到“思考中”
          if (meta.mode === 'kbqa_ai') {
            assistantMsg.waitingText = 'AI 正在思考…'
          }
        }
        highlightIds.value = meta.relatedNodeIds || []
        if (highlightIds.value.length > 0) {
          await loadRelatedGraph(highlightIds.value)
        }
      },
      onDelta: (content) => {
        // 首个 token 到达后，气泡内的等待动画自然被内容替换
        assistantMsg.content += content
        scrollBottom()
      },
      onDone: () => {
        assistantMsg.streaming = false
        loading.value = false
        saveQaRecord(q)
      },
      onError: (message) => {
        assistantMsg.streaming = false
        loading.value = false
        if (!assistantMsg.content) {
          assistantMsg.content = '抱歉，回答出现问题：' + (message || '请稍后重试。')
        }
      }
    })
  } finally {
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
  // Markdown（表格/列表/标题等）+ KaTeX 公式（$...$ / $$...$$）渲染
  return renderMarkdown(text)
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
  .msg-content {
    word-break: break-word;

    :deep(p) { margin: 6px 0; }
    :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5) { margin: 10px 0 6px; line-height: 1.4; }
    :deep(ul), :deep(ol) { margin: 6px 0; padding-left: 20px; }
    :deep(li) { margin: 2px 0; }
    :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 10px 0; }
    :deep(blockquote) {
      margin: 6px 0; padding: 4px 12px;
      border-left: 3px solid var(--primary);
      color: var(--text-secondary);
    }
    :deep(a) { color: var(--primary); }
    :deep(code) {
      background: var(--bg-page);
      border: 1px solid var(--border);
      border-radius: 4px;
      padding: 1px 5px;
      font-size: 12px;
    }
    :deep(table) {
      border-collapse: collapse;
      margin: 8px 0;
      width: 100%;
      display: block;
      overflow-x: auto;
    }
    :deep(th), :deep(td) {
      border: 1px solid var(--border);
      padding: 6px 10px;
      text-align: left;
      white-space: nowrap;
    }
    :deep(th) { background: var(--bg-page); font-weight: 600; }
    :deep(.katex-display) {
      overflow-x: auto;
      overflow-y: hidden;
      padding: 4px 0;
      margin: 8px 0;
    }
  }
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
.typing-row {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
.waiting-text {
  color: var(--text-secondary);
  font-size: 13px;
  margin-left: 2px;
}
.stream-cursor {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--primary);
  animation: cursor-blink 0.9s step-end infinite;
}
@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
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
