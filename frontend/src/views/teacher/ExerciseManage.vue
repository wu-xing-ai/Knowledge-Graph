<template>
  <div class="page-container">
    <div class="page-header">
      <h2>习题与考点管理</h2>
      <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon>新增习题</el-button>
    </div>

    <el-table :data="exercises" stripe border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
      <el-table-column prop="type" label="题型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ typeMap[row.type] || row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="120">
        <template #default="{ row }">
          <el-rate v-model="row.difficulty" disabled :max="5" />
        </template>
      </el-table-column>
      <el-table-column prop="examOutline" label="考纲要求" width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" text>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑习题' : '新增习题'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="题型">
          <el-select v-model="form.type">
            <el-option label="选择题" value="CHOICE" />
            <el-option label="填空题" value="FILL" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="简答题" value="ESSAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" required>
          <el-input v-model="form.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="form.answer" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="考纲要求">
          <el-input v-model="form.examOutline" />
        </el-form-item>
        <el-form-item label="关联学科">
          <el-input v-model="form.subjectIds" placeholder="学科ID，逗号分隔" />
        </el-form-item>
        <el-form-item label="关联知识点">
          <el-input v-model="form.knowledgeIds" placeholder="知识点ID，逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getExercises, addExercise, updateExercise, deleteExercise } from '@/api/exercise'
import { ElMessage } from 'element-plus'

const exercises = ref([])
const dialogVisible = ref(false)
const typeMap = { CHOICE: '选择题', FILL: '填空题', JUDGE: '判断题', ESSAY: '简答题' }
const form = reactive({
  id: null, content: '', answer: '', analysis: '', type: 'CHOICE',
  difficulty: 3, examOutline: '', subjectIds: '', knowledgeIds: ''
})

async function fetchList() {
  const res = await getExercises()
  if (res.code === 200) exercises.value = res.data || []
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: null, content: '', answer: '', analysis: '', type: 'CHOICE',
      difficulty: 3, examOutline: '', subjectIds: '', knowledgeIds: ''
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.content) {
    ElMessage.warning('请填写题目内容')
    return
  }
  if (form.id) {
    await updateExercise(form)
  } else {
    await addExercise(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchList()
}

async function handleDelete(id) {
  await deleteExercise(id)
  ElMessage.success('已删除')
  fetchList()
}

onMounted(fetchList)
</script>
