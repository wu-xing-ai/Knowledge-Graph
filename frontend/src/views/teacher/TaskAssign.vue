<template>
  <div class="page-container">
    <div class="page-header">
      <h2>任务布置与完成情况</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- ==================== 布置任务 ==================== -->
      <el-tab-pane label="布置任务" name="assign">
        <el-card shadow="never" class="assign-card">
          <el-form :model="form" label-width="110px">
            <el-form-item label="任务标题" required>
              <el-input v-model="form.title" placeholder="如：牛顿第二定律预习任务" maxlength="100" />
            </el-form-item>

            <el-form-item label="内容类型" required>
              <el-radio-group v-model="form.itemType" @change="form.itemIds = []">
                <el-radio-button value="KNOWLEDGE">知识点</el-radio-button>
                <el-radio-button value="EXERCISE">习题</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item :label="form.itemType === 'KNOWLEDGE' ? '选择知识点' : '选择习题'" required>
              <el-select v-model="form.itemIds" multiple filterable collapse-tags collapse-tags-tooltip
                         :max-collapse-tags="3" style="width: 100%" placeholder="搜索并选择">
                <el-option v-for="item in itemOptions" :key="item.id"
                           :label="itemLabel(item)" :value="item.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="布置对象" required>
              <el-radio-group v-model="form.targetType" @change="form.targetIds = []">
                <el-radio-button value="CLASS">班级</el-radio-button>
                <el-radio-button value="GROUP">小组</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item :label="form.targetType === 'CLASS' ? '选择班级' : '选择小组'" required>
              <el-select v-model="form.targetIds" multiple filterable collapse-tags style="width: 100%"
                         placeholder="可多选">
                <el-option v-for="t in targetOptions" :key="t.id" :label="t.label" :value="t.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="截止时间">
              <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm"
                              placeholder="选填" style="width: 220px" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleAssign">
                <el-icon><Postcard /></el-icon>确认布置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ==================== 任务列表 ==================== -->
      <el-tab-pane label="任务列表" name="list">
        <el-table :data="assignments" stripe border>
          <el-table-column prop="title" label="任务标题" min-width="160" show-overflow-tooltip />
          <el-table-column label="内容" min-width="220">
            <template #default="{ row }">
              <el-tag size="small" :type="row.itemType === 'KNOWLEDGE' ? 'primary' : 'warning'" style="margin-right: 6px">
                {{ row.itemType === 'KNOWLEDGE' ? '知识点' : '习题' }}
              </el-tag>
              <span class="item-names">{{ (row.itemNames || []).join('、') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="布置对象" width="140">
            <template #default="{ row }">
              <el-tag size="small" :type="row.targetType === 'CLASS' ? 'success' : 'info'" style="margin-right: 4px">
                {{ row.targetType === 'CLASS' ? '班级' : '小组' }}
              </el-tag>
              {{ row.targetName }}
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="150">
            <template #default="{ row }">
              {{ row.deadline ? row.deadline.replace('T', ' ') : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="openRecords(row)">完成情况</el-button>
              <el-popconfirm title="撤销后学生将无法提交，确定？" @confirm="handleDeleteAssignment(row.id)">
                <template #reference>
                  <el-button type="danger" text>撤销</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 完成情况 -->
        <el-dialog v-model="recordsDialog" :title="`${currentAssignment?.title || ''} - 完成情况`" width="820px">
          <div class="records-summary">
            <el-tag :type="'primary'" style="margin-right: 8px">已提交 {{ submittedCount }}</el-tag>
            <el-tag :type="'info'">未完成 {{ pendingCount }}</el-tag>
          </div>
          <el-table :data="records" stripe border size="small">
            <el-table-column prop="userName" label="学生" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="140">
              <template #default="{ row }">
                <template v-if="row.status === 'SUBMITTED'">
                  <el-input-number v-model="row.score" :min="0" :max="100" size="small" style="width: 110px" />
                </template>
                <template v-else>
                  {{ row.score ?? '—' }}
                </template>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="160">
              <template #default="{ row }">
                {{ row.submitTime ? row.submitTime.replace('T', ' ') : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="answer" label="提交内容" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'SUBMITTED'" type="primary" text size="small"
                           @click="handleGrade(row)">保存成绩</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { queryKnowledge } from '@/api/knowledge'
import { getExercises } from '@/api/exercise'
import {
  getClasses, getGroups,
  getAssignments, createAssignment, deleteAssignment,
  getAssignmentRecords, gradeRecord
} from '@/api/teaching'

const activeTab = ref('assign')

const form = reactive({
  title: '', itemType: 'KNOWLEDGE', itemIds: [],
  targetType: 'CLASS', targetIds: [], deadline: ''
})
const submitting = ref(false)

const knowledgeList = ref([])
const exerciseList = ref([])
const classes = ref([])
const groups = ref([])

const itemOptions = computed(() =>
  form.itemType === 'KNOWLEDGE' ? knowledgeList.value : exerciseList.value
)

const targetOptions = computed(() => {
  if (form.targetType === 'CLASS') {
    return classes.value.map(c => ({ id: c.id, label: c.name }))
  }
  const gs = form.targetType === 'GROUP' ? groups.value : []
  return gs.map(g => ({ id: g.id, label: `${g.clazzName || ''} · ${g.name}` }))
})

function itemLabel(item) {
  if (form.itemType === 'KNOWLEDGE') return `[${item.subject || '知识点'}] ${item.name}`
  return item.content
}

async function loadItems() {
  const [kRes, eRes] = await Promise.all([queryKnowledge({}), getExercises()])
  if (kRes.code === 200) knowledgeList.value = kRes.data || []
  if (eRes.code === 200) exerciseList.value = eRes.data || []
}

async function loadTargets() {
  const [cRes, gRes] = await Promise.all([getClasses(), getGroups(null)])
  if (cRes.code === 200) classes.value = cRes.data || []
  if (gRes.code === 200) groups.value = gRes.data || []
}

async function handleAssign() {
  if (!form.title) {
    ElMessage.warning('请填写任务标题')
    return
  }
  if (!form.itemIds.length) {
    ElMessage.warning('请选择任务内容')
    return
  }
  if (!form.targetIds.length) {
    ElMessage.warning('请选择布置对象')
    return
  }
  submitting.value = true
  try {
    const itemIds = form.itemIds.join(',')
    for (const targetId of form.targetIds) {
      await createAssignment({
        title: form.title,
        itemType: form.itemType,
        itemIds,
        targetType: form.targetType,
        targetId,
        deadline: form.deadline || null
      })
    }
    ElMessage.success(`已布置给 ${form.targetIds.length} 个${form.targetType === 'CLASS' ? '班级' : '小组'}`)
    form.title = ''
    form.itemIds = []
    form.targetIds = []
    form.deadline = ''
    fetchAssignments()
  } finally {
    submitting.value = false
  }
}

// ---------- 任务列表 ----------
const assignments = ref([])

async function fetchAssignments() {
  const res = await getAssignments()
  if (res.code === 200) assignments.value = res.data || []
}

async function handleDeleteAssignment(id) {
  await deleteAssignment(id)
  ElMessage.success('已撤销')
  fetchAssignments()
}

// ---------- 完成情况 ----------
const recordsDialog = ref(false)
const currentAssignment = ref(null)
const records = ref([])

function statusType(s) {
  return s === 'GRADED' ? 'success' : s === 'SUBMITTED' ? 'primary' : 'info'
}
function statusText(s) {
  return s === 'GRADED' ? '已批改' : s === 'SUBMITTED' ? '已提交' : '未完成'
}

const submittedCount = computed(() => records.value.filter(r => r.status !== 'PENDING').length)
const pendingCount = computed(() => records.value.filter(r => r.status === 'PENDING').length)

async function openRecords(row) {
  currentAssignment.value = row
  recordsDialog.value = true
  const res = await getAssignmentRecords(row.id)
  if (res.code === 200) records.value = res.data || []
}

async function handleGrade(row) {
  await gradeRecord(currentAssignment.value.id, row.userId, row.score)
  ElMessage.success('成绩已保存')
}
</script>

<style scoped lang="scss">
.assign-card {
  max-width: 720px;
  border-radius: 14px;
}

.item-names {
  font-size: 12px;
  color: var(--text-secondary);
}

.records-summary {
  margin-bottom: 12px;
}
</style>