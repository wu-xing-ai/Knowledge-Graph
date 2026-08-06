<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的任务</h2>
      <div class="summary">
        <el-tag type="primary" style="margin-right: 8px">待完成 {{ pendingCount }}</el-tag>
        <el-tag type="success" style="margin-right: 8px">已提交 {{ submittedCount }}</el-tag>
        <el-tag type="info">已批改 {{ gradedCount }}</el-tag>
      </div>
    </div>

    <el-empty v-if="!loading && tasks.length === 0" description="暂无布置给你的任务" />

    <div class="task-list" v-loading="loading">
      <el-card v-for="task in tasks" :key="task.id" shadow="hover" class="task-card">
        <div class="task-head">
          <div class="task-title">
            <el-tag :type="task.status === 'GRADED' ? 'success' : task.status === 'SUBMITTED' ? 'primary' : 'danger'"
                    size="small" style="margin-right: 8px">
              {{ statusText(task.status) }}
            </el-tag>
            <span class="title-text">{{ task.title }}</span>
          </div>
          <div class="task-meta">
            <span>布置老师：{{ task.teacherName || '—' }}</span>
            <span>对象：{{ task.targetName }}</span>
            <span :class="{ overdue: isOverdue(task) }">
              截止：{{ task.deadline ? task.deadline.replace('T', ' ') : '不限' }}
            </span>
          </div>
        </div>

        <div class="task-body">
          <div class="task-items">
            <div class="items-label">
              <el-tag size="small" :type="task.itemType === 'KNOWLEDGE' ? 'primary' : 'warning'">
                {{ task.itemType === 'KNOWLEDGE' ? '知识点' : '习题' }}
              </el-tag>
              <span class="items-hint">{{ task.itemNames.length }} 项</span>
            </div>
            <ul class="item-names">
              <li v-for="(name, i) in task.itemNames" :key="i">{{ i + 1 }}. {{ name }}</li>
            </ul>
          </div>

          <div class="task-actions">
            <template v-if="task.status === 'PENDING'">
              <el-button type="primary" @click="openSubmit(task)">去完成</el-button>
            </template>
            <template v-else>
              <el-tag v-if="task.score !== null && task.score !== undefined" size="large" type="success">
                得分：{{ task.score }}
              </el-tag>
              <el-tag v-else size="large" type="primary">已提交</el-tag>
              <div class="submit-time" v-if="task.submitTime">
                {{ task.submitTime.replace('T', ' ') }}
              </div>
            </template>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 提交弹窗 -->
    <el-dialog v-model="submitDialog" :title="`提交任务：${currentTask?.title || ''}`" width="560px">
      <div class="task-brief" v-if="currentTask">
        <el-tag size="small" :type="currentTask.itemType === 'KNOWLEDGE' ? 'primary' : 'warning'">
          {{ currentTask.itemType === 'KNOWLEDGE' ? '知识点' : '习题' }}
        </el-tag>
        <div class="brief-items">
          <p v-for="(name, i) in currentTask.itemNames" :key="i">{{ i + 1 }}. {{ name }}</p>
        </div>
      </div>
      <el-input v-model="submitAnswer" type="textarea" :rows="6" placeholder="填写你的完成情况 / 答案 / 学习心得" />
      <template #footer>
        <el-button @click="submitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyTasks, submitTask } from '@/api/teaching'

const tasks = ref([])
const loading = ref(false)
const submitDialog = ref(false)
const currentTask = ref(null)
const submitAnswer = ref('')
const submitting = ref(false)

function statusText(s) {
  return s === 'GRADED' ? '已批改' : s === 'SUBMITTED' ? '已提交' : '待完成'
}

function isOverdue(task) {
  if (!task.deadline || task.status !== 'PENDING') return false
  return new Date(task.deadline.replace('T', ' ').replace(' ', 'T')) < new Date()
}

const pendingCount = computed(() => tasks.value.filter(t => t.status === 'PENDING').length)
const submittedCount = computed(() => tasks.value.filter(t => t.status === 'SUBMITTED').length)
const gradedCount = computed(() => tasks.value.filter(t => t.status === 'GRADED').length)

async function fetchTasks() {
  loading.value = true
  try {
    const res = await getMyTasks()
    if (res.code === 200) tasks.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openSubmit(task) {
  currentTask.value = task
  submitAnswer.value = ''
  submitDialog.value = true
}

async function handleSubmit() {
  if (!submitAnswer.value.trim()) {
    ElMessage.warning('请填写完成情况')
    return
  }
  submitting.value = true
  try {
    await submitTask(currentTask.value.id, submitAnswer.value)
    ElMessage.success('提交成功')
    submitDialog.value = false
    fetchTasks()
  } finally {
    submitting.value = false
  }
}

onMounted(fetchTasks)
</script>

<style scoped lang="scss">
.summary {
  display: flex;
  align-items: center;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 120px;
}

.task-card {
  border-radius: 14px;

  .task-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 12px;

    .task-title {
      display: flex;
      align-items: center;
      font-size: 15px;
      font-weight: 600;

      .title-text {
        color: var(--text-primary);
      }
    }

    .task-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: var(--text-muted);

      .overdue {
        color: #f56c6c;
      }
    }
  }

  .task-body {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    flex-wrap: wrap;

    .task-items {
      flex: 1;
      min-width: 260px;

      .items-label {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 6px;

        .items-hint {
          font-size: 12px;
          color: var(--text-muted);
        }
      }

      .item-names {
        margin: 0;
        padding-left: 18px;
        font-size: 13px;
        color: var(--text-secondary);
        line-height: 1.9;
      }
    }

    .task-actions {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 6px;

      .submit-time {
        font-size: 12px;
        color: var(--text-muted);
      }
    }
  }
}

.task-brief {
  margin-bottom: 14px;

  .brief-items {
    margin-top: 8px;
    padding-left: 18px;
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.9;
  }
}
</style>