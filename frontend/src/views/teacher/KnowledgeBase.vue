<template>
  <div class="page-container">
    <div class="page-header">
      <h2>交叉学科知识库管理</h2>
      <div style="display:flex;gap:8px;">
        <el-upload :show-file-list="false" :before-upload="handleImport" accept=".xlsx,.xls">
          <el-button type="success"><el-icon><Upload /></el-icon>批量导入</el-button>
        </el-upload>
        <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon>新增知识点</el-button>
      </div>
    </div>

    <!-- 筛选 -->
    <el-card shadow="never" style="margin-bottom:16px;">
      <div style="display:flex;gap:12px;flex-wrap:wrap;">
        <el-select v-model="query.subjectId" placeholder="学科" clearable style="width:180px;" @change="fetchList">
          <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索知识点" style="width:200px;" clearable @keyup.enter="fetchList" />
        <el-button type="primary" @click="fetchList">查询</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-table :data="knowledgeList" stripe border>
      <el-table-column prop="name" label="知识点名称" width="200" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="subject" label="学科" width="120">
        <template #default="{ row }">
          <el-tag :color="getSubjectColor(row.subject)" effect="dark" size="small" style="color:#fff;">
            {{ row.subject }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="120">
        <template #default="{ row }">
          <el-rate v-model="row.difficulty" disabled :max="5" />
        </template>
      </el-table-column>
      <el-table-column prop="isCross" label="跨学科" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isCross" type="warning" size="small">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识点' : '新增知识点'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type">
            <el-option label="概念" value="概念" />
            <el-option label="公式" value="公式" />
            <el-option label="实验" value="实验" />
            <el-option label="考点" value="考点" />
            <el-option label="拓展素材" value="拓展素材" />
            <el-option label="交叉应用案例" value="交叉应用案例" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属学科" required>
          <el-select v-model="form.subjectId">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty" :max="5" />
        </el-form-item>
        <el-form-item label="跨学科">
          <el-switch v-model="form.isCross" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔" />
        </el-form-item>
        <el-form-item label="考纲要求">
          <el-input v-model="form.examOutline" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { queryKnowledge, addKnowledge, updateKnowledge, deleteKnowledge, getSubjects, batchImport } from '@/api/knowledge'
import { ElMessage } from 'element-plus'

const subjects = ref([])
const knowledgeList = ref([])
const dialogVisible = ref(false)
const query = reactive({ subjectId: null, keyword: '' })
const form = reactive({
  id: null, name: '', type: '概念', subjectId: null, subject: '',
  difficulty: 3, isCross: false, tags: '', examOutline: '', description: ''
})

const subjectColorMap = {
  '高中物理': '#4A90D9', '高中化学': '#F5A623', '高中生物': '#7ED321',
  '航天拓展': '#9B59B6', '环境科普': '#50E3C2'
}
function getSubjectColor(name) {
  return subjectColorMap[name] || '#BDC3C7'
}

async function fetchList() {
  const params = {}
  if (query.subjectId) params.subjectId = query.subjectId
  if (query.keyword) params.keyword = query.keyword
  const res = await queryKnowledge(params)
  if (res.code === 200) knowledgeList.value = res.data || []
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: null, name: '', type: '概念', subjectId: null, subject: '',
      difficulty: 3, isCross: false, tags: '', examOutline: '', description: ''
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.subjectId) {
    ElMessage.warning('请填写必要信息')
    return
  }
  const subject = subjects.value.find(s => s.id === form.subjectId)
  form.subject = subject?.name || ''

  if (form.id) {
    await updateKnowledge(form)
  } else {
    await addKnowledge(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  fetchList()
}

async function handleDelete(id) {
  await deleteKnowledge(id)
  ElMessage.success('已删除')
  fetchList()
}

async function handleImport(file) {
  try {
    const res = await batchImport(file)
    ElMessage.success(res.data || '导入成功')
    fetchList()
  } catch (e) {
    ElMessage.error('导入失败')
  }
  return false
}

onMounted(async () => {
  const res = await getSubjects()
  if (res.code === 200) subjects.value = res.data
  fetchList()
})
</script>
