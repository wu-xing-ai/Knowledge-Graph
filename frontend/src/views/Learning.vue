<template>
  <div class="page-container">
    <div class="page-header">
      <h2>跨学科学习辅助</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 刷题 -->
      <el-tab-pane label="刷题" name="practice">
        <el-card shadow="never">
          <div style="display:flex;gap:12px;margin-bottom:16px;">
            <el-select v-model="practiceSubject" placeholder="选择学科（默认全部）" clearable style="width:200px;">
              <el-option label="全部" :value="null" />
              <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-button type="primary" :loading="practiceLoading" @click="startPractice">开始刷题</el-button>
          </div>

          <template v-if="practiceQuestions.length">
            <!-- 完成态 -->
            <div v-if="practiceIndex >= practiceQuestions.length" style="text-align:center;padding:32px 0;">
              <el-result icon="success" title="本组练习完成"
                         :sub-title="`共 ${practiceQuestions.length} 题，答对 ${practiceCorrectCount} 题`">
                <template #extra>
                  <el-button type="primary" @click="startPractice">再来一组</el-button>
                </template>
              </el-result>
            </div>

            <!-- 逐题卡片 -->
            <div v-else>
              <div style="color:var(--text-secondary);font-size:13px;margin-bottom:8px;">
                第 {{ practiceIndex + 1 }} / {{ practiceQuestions.length }} 题
                <el-tag size="small" style="margin-left:8px;">{{ typeMap[currentQuestion.type] || currentQuestion.type }}</el-tag>
              </div>
              <el-card shadow="hover" style="margin-bottom:12px;">
                <p style="white-space:pre-wrap;line-height:1.8;">{{ currentQuestion.content }}</p>
              </el-card>

              <template v-if="!currentResult">
                <div style="display:flex;gap:12px;">
                  <el-input v-model="practiceAnswer" placeholder="选择题填选项字母，如 A"
                            style="max-width:320px;" @keyup.enter="submitAnswer" />
                  <el-button type="primary" :disabled="!practiceAnswer" @click="submitAnswer">提交</el-button>
                </div>
              </template>
              <template v-else>
                <el-alert :type="currentResult.correct ? 'success' : 'error'" :closable="false"
                          :title="currentResult.correct ? '回答正确' : '回答错误'" style="margin-bottom:12px;" />
                <p><b>标准答案：</b>{{ currentResult.answer || '（无）' }}</p>
                <p v-if="currentResult.analysis" style="color:var(--text-secondary);">
                  <b>解析：</b>{{ currentResult.analysis }}
                </p>
                <el-button type="primary" style="margin-top:8px;" @click="nextQuestion">
                  {{ practiceIndex + 1 >= practiceQuestions.length ? '查看结果' : '下一题' }}
                </el-button>
              </template>
            </div>
          </template>

          <el-empty v-else description="选择学科后点击「开始刷题」" />
        </el-card>
      </el-tab-pane>

      <!-- 知识溯源 -->
      <el-tab-pane label="知识溯源" name="trace">
        <el-card shadow="never">
          <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;">
            <el-select v-model="traceSubject" placeholder="按学科筛选（可选）" clearable style="width:220px;" @change="loadTraceNodes">
              <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-select v-model="traceNode" placeholder="选择起始知识点" filterable style="min-width:280px;">
              <el-option v-for="n in traceNodes" :key="n.id" :label="n.name" :value="n.id" />
            </el-select>
            <el-button type="primary" :disabled="!traceNode" @click="doTrace">溯源</el-button>
          </div>
          <KnowledgeGraph v-if="traceData.nodes.length" :data="traceData" height="400px" />
          <el-empty v-else description="选择起始知识点后，追溯其前置知识 / 原理来源" />
        </el-card>
        <el-card shadow="never" style="margin-top:16px;">
          <template #header>
            <b>跨学科关联图谱生成</b>
          </template>
          <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;">
            <el-input v-model="generateKeywords" placeholder="输入关键词，用逗号分隔，如：酶催化、催化剂"
                      clearable style="min-width:360px;flex:1;" @keyup.enter="generateGraphByKeywords" />
            <el-button type="primary" :loading="generating" @click="generateGraphByKeywords">生成关联图谱</el-button>
          </div>
          <KnowledgeGraph v-if="generatedData.nodes.length" :data="generatedData" height="420px" />
          <el-empty v-else description="输入多个关键词，自动展示它们之间的关联关系" />
        </el-card>
      </el-tab-pane>

      <!-- 知识点收藏 -->
      <el-tab-pane label="收藏" name="favorite">
        <div v-if="favorites.length" class="card-grid">
          <el-card v-for="fav in favorites" :key="fav.id" shadow="hover">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span style="font-weight:600;">{{ fav.knowledgeName }}</span>
                <el-button type="danger" text @click="removeFavorite(fav.id)">取消收藏</el-button>
              </div>
            </template>
            <p style="color:var(--text-secondary);font-size:13px;">{{ fav.category || '未分类' }}</p>
          </el-card>
        </div>
        <el-empty v-else description="暂无收藏" />
      </el-tab-pane>

      <!-- 笔记 -->
      <el-tab-pane label="笔记" name="note">
        <div style="margin-bottom:12px;">
          <el-button type="primary" @click="showNoteDialog = true">新建笔记</el-button>
        </div>
        <el-table :data="notes" stripe>
          <el-table-column prop="knowledgeName" label="关联知识点" width="200" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="danger" text @click="removeNote(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 笔记弹窗 -->
    <el-dialog v-model="showNoteDialog" title="新建笔记" width="500px">
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="知识点">
          <el-input v-model="noteForm.knowledgeName" placeholder="关联知识点名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="noteForm.category" placeholder="如：航天交叉" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="noteForm.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteDialog = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getTrace, generateGraph } from '@/api/graph'
import { getSubjects, queryKnowledge } from '@/api/knowledge'
import { getNotes, addNote, deleteNote, getFavorites, deleteFavorite, getPracticeQuestions, submitPractice } from '@/api/exercise'
import { useUserStore } from '@/store/user'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('practice')
const subjects = ref([])
const traceSubject = ref(null)
const traceNode = ref(null)
const traceNodes = ref([])
const traceData = ref({ nodes: [], edges: [] })
const generateKeywords = ref('')
const generating = ref(false)
const generatedData = ref({ nodes: [], edges: [] })
const favorites = ref([])
const notes = ref([])
const showNoteDialog = ref(false)
const noteForm = reactive({ knowledgeName: '', category: '', content: '' })

// 刷题
const typeMap = { CHOICE: '选择题', FILL: '填空题', JUDGE: '判断题', ESSAY: '简答题' }
const practiceSubject = ref(null)
const practiceLoading = ref(false)
const practiceQuestions = ref([])
const practiceIndex = ref(0)
const practiceAnswer = ref('')
const currentResult = ref(null)
const practiceCorrectCount = ref(0)

const currentQuestion = computed(() => practiceQuestions.value[practiceIndex.value] || {})

async function startPractice() {
  practiceLoading.value = true
  try {
    const res = await getPracticeQuestions({ subjectId: practiceSubject.value, count: 10 })
    if (res.code === 200) {
      practiceQuestions.value = res.data || []
      practiceIndex.value = 0
      practiceAnswer.value = ''
      currentResult.value = null
      practiceCorrectCount.value = 0
      if (!practiceQuestions.value.length) {
        ElMessage.warning('该学科暂无习题')
      }
    }
  } finally {
    practiceLoading.value = false
  }
}

async function submitAnswer() {
  const res = await submitPractice({
    userId: userStore.user?.userId,
    exerciseId: currentQuestion.value.id,
    answer: practiceAnswer.value
  })
  if (res.code === 200) {
    currentResult.value = res.data
    if (res.data.correct) practiceCorrectCount.value++
  }
}

function nextQuestion() {
  practiceIndex.value++
  practiceAnswer.value = ''
  currentResult.value = null
}

async function loadTraceNodes() {
  const params = {}
  if (traceSubject.value) params.subjectId = traceSubject.value
  const res = await queryKnowledge(params)
  if (res.code === 200) {
    traceNodes.value = res.data || []
    if (!traceNodes.value.some(n => n.id === traceNode.value)) traceNode.value = null
  }
}

async function doTrace() {
  if (!traceNode.value) return
  const res = await getTrace(traceNode.value, 3)
  if (res.code === 200) traceData.value = res.data
}

async function generateGraphByKeywords() {
  const keywords = generateKeywords.value
    .split(/[,，;；]/)
    .map(item => item.trim())
    .filter(Boolean)
  if (!keywords.length) {
    ElMessage.warning('请输入至少一个关键词')
    return
  }
  generating.value = true
  try {
    const res = await generateGraph(keywords, 30)
    if (res.code === 200) {
      generatedData.value = res.data || { nodes: [], edges: [] }
      if (!generatedData.value.nodes.length) {
        ElMessage.info('未匹配到知识点')
      }
    }
  } finally {
    generating.value = false
  }
}

async function loadNotes() {
  const res = await getNotes(userStore.user?.userId)
  if (res.code === 200) {
    notes.value = res.data || []
  }
}

async function saveNote() {
  if (!noteForm.content) return
  await addNote({ ...noteForm, userId: userStore.user?.userId })
  ElMessage.success('笔记已保存')
  showNoteDialog.value = false
  noteForm.knowledgeName = ''
  noteForm.category = ''
  noteForm.content = ''
  loadNotes()
}

async function removeNote(id) {
  await deleteNote(id)
  ElMessage.success('已删除')
  loadNotes()
}

async function loadFavorites() {
  const res = await getFavorites(userStore.user?.userId)
  if (res.code === 200) {
    favorites.value = res.data || []
  }
}

async function removeFavorite(id) {
  await deleteFavorite(id)
  ElMessage.success('已取消收藏')
  loadFavorites()
}

onMounted(async () => {
  const res = await getSubjects()
  if (res.code === 200) subjects.value = res.data
  loadTraceNodes()
  loadNotes()
  loadFavorites()
})
</script>
