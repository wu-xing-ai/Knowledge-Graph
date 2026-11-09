<template>
  <div class="page-container">
    <div class="page-header">
      <h2>交叉学科知识库管理</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- ========== 知识点管理 ========== -->
      <el-tab-pane label="知识点" name="knowledge">
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-bottom:12px;">
          <el-upload :show-file-list="false" :before-upload="handleImport" accept=".xlsx,.xls">
            <el-button type="success"><el-icon><Upload /></el-icon>批量导入</el-button>
          <el-button @click="handleDownloadTemplate"><el-icon><Download /></el-icon>下载模板</el-button>
          </el-upload>
          <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon>新增知识点</el-button>
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
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <div class="op-cell">
                <el-button type="primary" text @click="openDialog(row)">编辑</el-button>
                <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
                  <template #reference>
                    <el-button type="danger" text>删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ========== 交叉关系管理 ========== -->
      <el-tab-pane label="交叉关系" name="relations">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
          <span style="color:var(--text-secondary);font-size:13px;">
            共 {{ relations.length }} 条交叉关系 · 关系类型：{{ relationTypeOptions.join('、') }} 等
          </span>
          <el-button type="primary" @click="openRelationDialog()"><el-icon><Connection /></el-icon>新增关系</el-button>
        </div>

        <el-table :data="relations" stripe border>
          <el-table-column prop="sourceName" label="源知识点" min-width="160" />
          <el-table-column prop="relationType" label="关系类型" width="150">
            <template #default="{ row }">
              <el-tag type="warning" size="small">{{ row.relationType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetName" label="目标知识点" min-width="160" />
          <el-table-column prop="description" label="关系说明" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该关系？" @confirm="handleDeleteRelation(row)">
                <template #reference>
                  <el-button type="danger" text>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!relations.length" description="暂无交叉关系，点击右上角新增" :image-size="80" />
      </el-tab-pane>
    
      <!-- ========== 文档导入（Word/PDF/图片 -> 知识抽取） ========== -->
      <el-tab-pane label="文档导入" name="document">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex-wrap:wrap;gap:8px;">
          <span style="color:var(--text-secondary);font-size:13px;">
            支持 Word(.docx)/PDF/文本/图片，扫描件自动云端 OCR，再经 LLM 抽取知识点与交叉关系，确认后写入图谱
          </span>
          <el-upload :show-file-list="false" :before-upload="handleExtract" accept=".pdf,.docx,.txt,.md,.png,.jpg,.jpeg,.bmp,.webp">
            <el-button type="primary" :loading="extracting">
              <el-icon><Upload /></el-icon>{{ extracting ? '解析抽取中(OCR+LLM约10~60秒)...' : '上传并抽取' }}
            </el-button>
          </el-upload>
        </div>

        <!-- 解析进度条（玻璃态） -->
        <div v-if="extracting" class="doc-progress">
          <div class="shape shape-1"></div>
          <div class="shape shape-2"></div>
          <div class="shape shape-3"></div>
          <div class="glass-card">
            <div class="loading-text">正在解析文档…</div>
            <div class="progress-container">
              <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <div class="progress-tip">{{ progressMessage }}（{{ progressPercent }}%）</div>
          </div>
        </div>

        <template v-if="extractResult">
          <el-alert type="success" :closable="false" style="margin-bottom:12px;">
            《{{ extractResult.fileName }}》 解析来源：{{ extractResult.source }} · {{ extractResult.charCount }} 字
          </el-alert>

          <el-card shadow="never" style="margin-bottom:16px;">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span style="font-weight:600;">① 抽取的知识点（已选 {{ selectedEntities.length }} / {{ extractResult.entities.length }}）</span>
                <span>
                  <el-button type="primary" text @click="setAllEntities(true)">全选</el-button>
                  <el-button text @click="setAllEntities(false)">全不选</el-button>
                </span>
              </div>
            </template>
            <el-table :data="extractResult.entities" stripe border @selection-change="onSelectEntities" ref="entityTableRef" max-height="320">
              <el-table-column type="selection" width="45" />
              <el-table-column prop="name" label="名称" min-width="160" />
              <el-table-column prop="type" label="类型" width="110">
                <template #default="{ row }"><el-tag size="small">{{ row.type }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="subject" label="学科" width="120">
                <template #default="{ row }">
                  <el-tag :color="getSubjectColor(row.subject)" effect="dark" size="small" style="color:#fff;">{{ row.subject }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" show-overflow-tooltip />
            </el-table>
            <el-empty v-if="!extractResult.entities.length" description="未抽取到知识点" :image-size="60" />
          </el-card>

          <el-card shadow="never" style="margin-bottom:16px;">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <span style="font-weight:600;">② 抽取的交叉关系（已选 {{ selectedRelations.length }} / {{ extractResult.relations.length }}）</span>
                <span>
                  <el-button type="primary" text @click="setAllRelations(true)">全选</el-button>
                  <el-button text @click="setAllRelations(false)">全不选</el-button>
                </span>
              </div>
            </template>
            <el-table :data="extractResult.relations" stripe border @selection-change="onSelectRelations" ref="relationTableRef" max-height="260">
              <el-table-column type="selection" width="45" />
              <el-table-column prop="source" label="源知识点" min-width="150" />
              <el-table-column prop="relationType" label="关系" width="120">
                <template #default="{ row }"><el-tag type="warning" size="small">{{ row.relationType }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="target" label="目标知识点" min-width="150" />
              <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
            </el-table>
            <el-empty v-if="!extractResult.relations.length" description="未抽取到关系" :image-size="60" />
          </el-card>

          <div style="display:flex;justify-content:flex-end;gap:8px;">
            <el-button @click="extractResult = null">清空</el-button>
            <el-button type="primary" :loading="importing" :disabled="!selectedEntities.length && !selectedRelations.length" @click="handleImportDoc">
              <el-icon><Check /></el-icon>确认导入（{{ selectedEntities.length }} 个知识点 / {{ selectedRelations.length }} 条关系）
            </el-button>
          </div>

          <el-collapse style="margin-top:16px;">
            <el-collapse-item title="查看提取的原文文本" name="text">
              <pre style="white-space:pre-wrap;font-size:13px;line-height:1.8;color:var(--text-secondary);max-height:320px;overflow:auto;">{{ extractResult.text }}</pre>
            </el-collapse-item>
          </el-collapse>
        </template>

        <el-empty v-else description="上传文档后自动抽取知识点与交叉关系，确认后写入知识图谱" :image-size="120" />

        <!-- 待确认导入的草稿（切换页面不丢失） -->
        <div v-if="docDrafts.length" style="margin-top:24px;">
          <h4 style="margin-bottom:12px;color:var(--text-primary);">待确认导入</h4>
          <el-table :data="docDrafts" stripe border>
            <el-table-column prop="fileName" label="文档" min-width="200" show-overflow-tooltip />
            <el-table-column prop="source" label="解析来源" width="130" />
            <el-table-column prop="entityCount" label="知识点" width="90" />
            <el-table-column prop="relationCount" label="关系" width="80" />
            <el-table-column prop="createTime" label="抽取时间" width="180" />
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <div class="op-cell">
                  <el-button type="primary" text @click="viewDraft(row)">查看并导入</el-button>
                  <el-popconfirm title="删除该草稿？" @confirm="deleteDraft(row)">
                    <template #reference>
                      <el-button type="danger" text>删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <!-- 已导入的文档（可一键删除解析数据） -->
        <div v-if="docImports.length" style="margin-top:24px;">
          <h4 style="margin-bottom:12px;color:var(--text-primary);">已导入的文档</h4>
          <el-table :data="docImports" stripe border>
            <el-table-column prop="fileName" label="文档" min-width="220" show-overflow-tooltip />
            <el-table-column prop="nodeCount" label="新增知识点" width="110" />
            <el-table-column prop="relationCount" label="新增关系" width="100" />
            <el-table-column prop="createTime" label="导入时间" width="180" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="删除该文档及其解析出的知识点/关系？" @confirm="handleDeleteDoc(row)">
                  <template #reference>
                    <el-button type="danger" text>删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 知识点弹窗 -->
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

    <!-- 关系弹窗 -->
    <el-dialog v-model="relationDialogVisible" title="新增交叉关系" width="560px">
      <el-form :model="relationForm" label-width="100px">
        <el-form-item label="源知识点" required>
          <el-select v-model="relationForm.sourceId" filterable placeholder="选择源知识点" style="width:100%;">
            <el-option v-for="n in allKnowledge" :key="n.id" :label="n.name" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" required>
          <el-select v-model="relationForm.relationType" filterable allow-create default-first-option placeholder="选择或输入关系类型" style="width:100%;">
            <el-option v-for="t in relationTypeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标知识点" required>
          <el-select v-model="relationForm.targetId" filterable placeholder="选择目标知识点" style="width:100%;">
            <el-option v-for="n in allKnowledge" :key="n.id" :label="n.name" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系说明">
          <el-input v-model="relationForm.description" type="textarea" :rows="3" placeholder="如：生物中的酶催化与化学中的催化剂为共通原理" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRelation">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { queryKnowledge, addKnowledge, updateKnowledge, deleteKnowledge, getSubjects, batchImport, downloadTemplate, documentExtract, getExtractStatus, documentImport, getDocumentImports, deleteDocumentImport, getDocumentDrafts, getDocumentDraft, deleteDocumentDraft } from '@/api/knowledge'
import { getRelations, addRelation, deleteRelation } from '@/api/graph'
import { getSubjectColor } from '@/utils/subjectColors'
import { ElMessage } from 'element-plus'

const subjects = ref([])
const knowledgeList = ref([])
const allKnowledge = ref([])
const relations = ref([])
const dialogVisible = ref(false)
const relationDialogVisible = ref(false)
const activeTab = ref('knowledge')
const query = reactive({ subjectId: null, keyword: '' })
const form = reactive({
  id: null, name: '', type: '概念', subjectId: null, subject: '',
  difficulty: 3, isCross: false, tags: '', examOutline: '', description: ''
})
const relationForm = reactive({ sourceId: null, targetId: null, relationType: '跨学科关联', description: '' })

const relationTypeOptions = [
  '跨学科关联', '学科衔接', '共通原理', '交叉应用', '基于原理', '应用',
  '关联', '协同应用', '对立过程', '化学反应', '能量转换', '涉及', '推动', '前置知识'
]

async function fetchList() {
  const params = {}
  if (query.subjectId) params.subjectId = query.subjectId
  if (query.keyword) params.keyword = query.keyword
  const res = await queryKnowledge(params)
  if (res.code === 200) knowledgeList.value = res.data || []
}

async function loadAllKnowledge() {
  const res = await queryKnowledge({})
  if (res.code === 200) allKnowledge.value = res.data || []
}

async function loadRelations() {
  const res = await getRelations()
  if (res.code === 200) relations.value = res.data || []
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
  loadAllKnowledge()
}

async function handleDelete(id) {
  await deleteKnowledge(id)
  ElMessage.success('已删除')
  fetchList()
  loadAllKnowledge()
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '知识库导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('模板已下载，按示例填写后导入')
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

async function handleImport(file) {
  try {
    const res = await batchImport(file)
    ElMessage.success(res.data || '导入成功')
    fetchList()
    loadAllKnowledge()
  } catch (e) {
    ElMessage.error('导入失败')
  }
  return false
}

function openRelationDialog() {
  Object.assign(relationForm, { sourceId: null, targetId: null, relationType: '跨学科关联', description: '' })
  relationDialogVisible.value = true
}

async function handleSaveRelation() {
  if (!relationForm.sourceId || !relationForm.targetId || !relationForm.relationType) {
    ElMessage.warning('请填写完整的关系信息')
    return
  }
  if (relationForm.sourceId === relationForm.targetId) {
    ElMessage.warning('源知识点与目标知识点不能相同')
    return
  }
  try {
    const res = await addRelation(relationForm)
    if (res.code === 200) {
      ElMessage.success('关系已创建')
      relationDialogVisible.value = false
      loadRelations()
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败，请确认两个知识点都存在')
  }
}

async function handleDeleteRelation(row) {
  await deleteRelation(row.sourceId, row.targetId, row.relationType)
  ElMessage.success('已删除')
  loadRelations()
}

const extracting = ref(false)
const importing = ref(false)
const extractResult = ref(null)
const docImports = ref([])
const docDrafts = ref([])
const progressPercent = ref(0)
const progressMessage = ref('')
const selectedEntities = ref([])
const selectedRelations = ref([])
const entityTableRef = ref(null)
const relationTableRef = ref(null)

async function handleExtract(file) {
  extracting.value = true
  progressPercent.value = 5
  progressMessage.value = '任务已提交，开始解析文档...'
  try {
    const res = await documentExtract(file)
    if (res.code !== 200 || !res.data?.jobId) {
      ElMessage.error(res.message || '提交失败')
      extracting.value = false
      return false
    }
    const jobId = res.data.jobId
    const timer = setInterval(async () => {
      try {
        const st = await getExtractStatus(jobId)
        if (st.code === 200 && st.data) {
          progressPercent.value = st.data.percent || 0
          progressMessage.value = st.data.message || ''
          if (st.data.status === 'DONE') {
            clearInterval(timer)
            extractResult.value = st.data.result
            selectedEntities.value = []
            selectedRelations.value = []
            loadDrafts()
            extracting.value = false
            ElMessage.success('抽取完成，已存为草稿，可稍后确认导入')
          } else if (st.data.status === 'FAILED') {
            clearInterval(timer)
            extracting.value = false
            ElMessage.error(st.data.error || '抽取失败')
          }
        } else {
          clearInterval(timer)
          extracting.value = false
          ElMessage.error(st.message || '查询进度失败')
        }
      } catch (e) {
        clearInterval(timer)
        extracting.value = false
        ElMessage.error('查询抽取进度失败')
      }
    }, 4000)
  } catch (e) {
    extracting.value = false
    ElMessage.error(e.response?.data?.message || '提交失败，请检查后端日志')
  }
  return false
}

function onSelectEntities(rows) { selectedEntities.value = rows }
function onSelectRelations(rows) { selectedRelations.value = rows }
function setAllEntities(select) {
  extractResult.value?.entities.forEach(row => entityTableRef.value?.toggleRowSelection(row, select))
}
function setAllRelations(select) {
  extractResult.value?.relations.forEach(row => relationTableRef.value?.toggleRowSelection(row, select))
}


async function loadDrafts() {
  try {
    const res = await getDocumentDrafts()
    if (res.code === 200) docDrafts.value = res.data || []
  } catch (e) {
    // 忽略
  }
}

async function viewDraft(row) {
  try {
    const res = await getDocumentDraft(row.id)
    if (res.code === 200) {
      extractResult.value = res.data
      selectedEntities.value = []
      selectedRelations.value = []
    } else {
      ElMessage.error(res.message || '草稿加载失败')
    }
  } catch (e) {
    ElMessage.error('草稿加载失败')
  }
}

async function deleteDraft(row) {
  try {
    const res = await deleteDocumentDraft(row.id)
    if (res.code === 200) {
      ElMessage.success('草稿已删除')
      loadDrafts()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}
async function loadDocumentImports() {
  try {
    const res = await getDocumentImports()
    if (res.code === 200) docImports.value = res.data || []
  } catch (e) {
    // 忽略
  }
}

async function handleDeleteDoc(row) {
  try {
    const res = await deleteDocumentImport(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除该文档解析出的知识点与关系')
      loadDocumentImports()
      fetchList()
      loadAllKnowledge()
      loadRelations()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function handleImportDoc() {
  if (!selectedEntities.value.length && !selectedRelations.value.length) return
  importing.value = true
  try {
    const payload = {
      fileName: extractResult.value?.fileName,
      source: extractResult.value?.source,
      entities: selectedEntities.value,
      relations: selectedRelations.value
    }
    const res = await documentImport(payload)
    if (res.code === 200) {
      const s = res.data || {}
      ElMessage.success(`导入完成：新增知识点 ${s.createdNodes} 个、关系 ${s.createdRelations} 条（复用 ${s.existingNodes} 个）`)
      extractResult.value = null
      selectedEntities.value = []
      selectedRelations.value = []
      fetchList()
      loadAllKnowledge()
      loadRelations()
      loadDocumentImports()
      loadDrafts()
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '导入失败')
  } finally {
    importing.value = false
  }
}
onMounted(async () => {
  loadDocumentImports()
  loadDrafts()
  const res = await getSubjects()
  if (res.code === 200) subjects.value = res.data
  fetchList()
  loadAllKnowledge()
  loadRelations()
})
</script>

<style scoped lang="scss">
/* 操作列：固定为横向一排，避免表格出现纵向滚动条挤压列宽时按钮换行堆叠 */
.op-cell {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 4px;

  .el-button {
    margin-left: 0;
    padding: 4px 6px;
  }
}

.doc-progress {
  position: relative;
  width: 100%;
  max-width: 600px;
  margin: 24px auto;
  min-height: 240px;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 背景装饰圆球（品牌蓝 → 青色系，与全局氛围底一致） */
.shape {
  position: absolute;
  border-radius: 50%;
  z-index: 1;
}
.shape-1 {
  width: 140px; height: 140px;
  background: linear-gradient(135deg, #8BC1FF, #1783FF);
  top: -40px; right: 50px;
  filter: blur(2px);
}
.shape-2 {
  width: 220px; height: 220px;
  background: linear-gradient(135deg, #CFE5FF, #8FE3DB);
  bottom: -70px; left: -20px;
  filter: blur(4px);
}
.shape-3 {
  width: 70px; height: 70px;
  background: linear-gradient(135deg, #7dd3fc, #39B5A8);
  bottom: -110px; right: 140px;
  filter: blur(1px);
}

/* 玻璃态卡片 */
.glass-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 480px;
  padding: 50px 40px;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1.5px solid rgba(255, 255, 255, 0.8);
  border-radius: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.04),
              inset 0 2px 4px rgba(255, 255, 255, 0.4);
}

.loading-text {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 20px;
  background: linear-gradient(90deg, #1783FF, #0E6FE0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.progress-container {
  width: 100%;
  height: 18px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  width: 0%;
  background: linear-gradient(90deg, #1783FF, #39B5A8);
  border-radius: 20px;
  transition: width 0.6s ease;
  position: relative;
  overflow: hidden;
}
.progress-bar::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: shine 1.5s infinite;
}
.progress-tip {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}
@keyframes shine {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
</style>