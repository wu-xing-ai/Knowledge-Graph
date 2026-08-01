<template>
  <div class="page-container">
    <div class="page-header">
      <h2>跨学科学习辅助</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 知识溯源 -->
      <el-tab-pane label="知识溯源" name="trace">
        <el-card shadow="never">
          <div style="display:flex;gap:12px;margin-bottom:16px;">
            <el-select v-model="traceSubject" placeholder="选择学科" style="width:200px;">
              <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-button type="primary" @click="doTrace">溯源</el-button>
          </div>
          <KnowledgeGraph v-if="traceData.nodes.length" :data="traceData" height="400px" />
          <el-empty v-else description="选择学科进行知识溯源" />
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
import { ref, reactive, onMounted } from 'vue'
import { getGraphData } from '@/api/graph'
import { getSubjects } from '@/api/knowledge'
import { getNotes, addNote, deleteNote } from '@/api/exercise'
import { useUserStore } from '@/store/user'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('trace')
const subjects = ref([])
const traceSubject = ref(null)
const traceData = ref({ nodes: [], edges: [] })
const favorites = ref([])
const notes = ref([])
const showNoteDialog = ref(false)
const noteForm = reactive({ knowledgeName: '', category: '', content: '' })

async function doTrace() {
  if (!traceSubject.value) return
  const res = await getGraphData({ subjectIds: [traceSubject.value], limit: 50 })
  if (res.code === 200) traceData.value = res.data
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

function removeFavorite(id) {
  favorites.value = favorites.value.filter(f => f.id !== id)
}

onMounted(async () => {
  const res = await getSubjects()
  if (res.code === 200) subjects.value = res.data
  loadNotes()
})
</script>
