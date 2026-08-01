<template>
  <div class="page-container">
    <div class="page-header">
      <h2>个人学习中心</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 学习记录 -->
      <el-tab-pane label="学习记录" name="records">
        <el-timeline>
          <el-timeline-item
            v-for="r in records"
            :key="r.id"
            :timestamp="r.createTime"
            placement="top"
          >
            <el-card shadow="never">
              <span style="font-weight:600;">{{ actionMap[r.actionType] || r.actionType }}</span>
              <span style="color:var(--text-secondary);margin-left:8px;">{{ r.targetType || '' }}</span>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!records.length" description="暂无学习记录" />
      </el-tab-pane>

      <!-- 错题整理 -->
      <el-tab-pane label="错题整理" name="mistakes">
        <el-table :data="mistakes" stripe>
          <el-table-column prop="content" label="题目" show-overflow-tooltip />
          <el-table-column prop="type" label="题型" width="100" />
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-rate v-model="row.difficulty" disabled :max="5" />
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!mistakes.length" description="暂无错题" />
      </el-tab-pane>

      <!-- 我的笔记 -->
      <el-tab-pane label="我的笔记" name="notes">
        <el-table :data="notes" stripe>
          <el-table-column prop="knowledgeName" label="知识点" width="200" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
        <el-empty v-if="!notes.length" description="暂无笔记" />
      </el-tab-pane>

      <!-- 教师分享 -->
      <el-tab-pane label="学习资料" name="shared">
        <el-empty description="暂无教师分享的学习资料" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecords, getNotes } from '@/api/exercise'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const activeTab = ref('records')
const records = ref([])
const mistakes = ref([])
const notes = ref([])

const actionMap = {
  'PRACTICE': '刷题练习',
  'FAVORITE': '收藏知识点',
  'QA': '智能问答',
  'VIEW': '浏览知识图谱'
}

onMounted(async () => {
  const userId = userStore.user?.userId
  if (!userId) return

  const recordRes = await getRecords(userId)
  if (recordRes.code === 200) records.value = recordRes.data || []

  const noteRes = await getNotes(userId)
  if (noteRes.code === 200) notes.value = noteRes.data || []
})
</script>
