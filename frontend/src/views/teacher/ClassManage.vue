<template>
  <div class="page-container">
    <div class="page-header">
      <h2>班级与小组管理</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- ==================== 班级管理 ==================== -->
      <el-tab-pane label="班级管理" name="clazz">
        <div class="toolbar">
          <el-button type="primary" @click="openClazzDialog()">
            <el-icon><Plus /></el-icon>新建班级
          </el-button>
        </div>
        <el-table :data="classes" stripe border>
          <el-table-column prop="name" label="班级名称" min-width="140" />
          <el-table-column prop="grade" label="年级" width="100" />
          <el-table-column prop="school" label="学校" min-width="140" />
          <el-table-column prop="ownerName" label="班主任/教师" width="120" />
          <el-table-column prop="studentCount" label="学生数" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.studentCount > 0 ? 'primary' : 'info'">
                {{ row.studentCount }} 人
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="openStudents(row)">学生</el-button>
              <el-button type="primary" text @click="openMaterials(row)">资料</el-button>
              <el-button type="primary" text @click="openClazzDialog(row)">编辑</el-button>
              <el-popconfirm title="删除班级将同时移除学生与小组，确定？" @confirm="handleDeleteClazz(row.id)">
                <template #reference>
                  <el-button type="danger" text>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 班级新增/编辑 -->
        <el-dialog v-model="clazzDialog" :title="clazzForm.id ? '编辑班级' : '新建班级'" width="480px">
          <el-form :model="clazzForm" label-width="80px">
            <el-form-item label="班级名称" required>
              <el-input v-model="clazzForm.name" placeholder="如：高一(1)班" />
            </el-form-item>
            <el-form-item label="年级">
              <el-input v-model="clazzForm.grade" placeholder="如：高一" />
            </el-form-item>
            <el-form-item label="学校">
              <el-input v-model="clazzForm.school" placeholder="选填" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="clazzDialog = false">取消</el-button>
            <el-button type="primary" @click="handleSaveClazz">保存</el-button>
          </template>
        </el-dialog>

        <!-- 班级学生管理 -->
        <el-dialog v-model="studentsDialog" :title="`${currentClazz?.name || ''} - 学生管理`" width="720px">
          <div class="member-toolbar">
            <span>在班学生（{{ students.length }}）</span>
            <div class="add-member">
              <el-select v-model="newStudentId" placeholder="选择学生加入班级" filterable clearable style="width: 240px">
                <el-option v-for="s in candidates" :key="s.id"
                           :label="displayName(s)" :value="s.id" />
              </el-select>
              <el-button type="primary" :disabled="!newStudentId" @click="handleAddStudent">
                加入
              </el-button>
            </div>
          </div>
          <el-table :data="students" stripe border size="small">
            <el-table-column prop="username" label="账号" width="140" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="school" label="学校" min-width="140" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="将该学生移出班级？" @confirm="handleRemoveStudent(row.id)">
                  <template #reference>
                    <el-button type="danger" text size="small">移出</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-dialog>

        <!-- 班级学习资料管理 -->
        <el-dialog v-model="materialsDialog" :title="`${currentClazz?.name || ''} - 学习资料`" width="760px">
          <el-form :model="materialForm" label-width="80px" class="material-form">
            <el-form-item label="标题" required>
              <el-input v-model="materialForm.title" maxlength="200" placeholder="资料标题" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="materialForm.content" type="textarea" :rows="3"
                        placeholder="资料说明或链接文本" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveMaterial">
                {{ materialForm.id ? '保存修改' : '发布资料' }}
              </el-button>
              <el-button v-if="materialForm.id" @click="resetMaterialForm">取消编辑</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="materials" stripe border size="small">
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
            <el-table-column label="发布时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" text size="small" @click="editMaterial(row)">编辑</el-button>
                <el-popconfirm title="删除该资料？" @confirm="handleDeleteMaterial(row.id)">
                  <template #reference>
                    <el-button type="danger" text size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-dialog>
      </el-tab-pane>

      <!-- ==================== 小组管理 ==================== -->
      <el-tab-pane label="小组管理" name="group">
        <div class="toolbar">
          <el-select v-model="groupClazzId" placeholder="选择班级查看小组" clearable filterable
                     style="width: 200px" @change="fetchGroups">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button type="primary" :disabled="!groupClazzId" @click="openGroupDialog()">
            <el-icon><Plus /></el-icon>新建小组
          </el-button>
        </div>
        <el-table :data="groups" stripe border>
          <el-table-column prop="name" label="小组名称" min-width="140" />
          <el-table-column prop="clazzName" label="所属班级" min-width="120" />
          <el-table-column prop="leaderName" label="组长" width="110">
            <template #default="{ row }">
              {{ row.leaderName || '未设置' }}
            </template>
          </el-table-column>
          <el-table-column prop="memberCount" label="成员数" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.memberCount > 0 ? 'primary' : 'info'">
                {{ row.memberCount }} 人
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="openGroupMembers(row)">成员</el-button>
              <el-button type="primary" text @click="openGroupDialog(row)">编辑</el-button>
              <el-popconfirm title="删除小组将移除全部成员，确定？" @confirm="handleDeleteGroup(row.id)">
                <template #reference>
                  <el-button type="danger" text>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 小组新增/编辑 -->
        <el-dialog v-model="groupDialog" :title="groupForm.id ? '编辑小组' : '新建小组'" width="440px">
          <el-form :model="groupForm" label-width="80px">
            <el-form-item label="小组名称" required>
              <el-input v-model="groupForm.name" placeholder="如：火箭小组" />
            </el-form-item>
            <el-form-item label="所属班级" required>
              <el-select v-model="groupForm.clazzId" style="width: 100%">
                <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="组长">
              <el-select v-model="groupForm.leaderId" placeholder="从班内学生中选" filterable clearable style="width: 100%">
                <el-option v-for="s in groupLeaders" :key="s.id" :label="displayName(s)" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="groupDialog = false">取消</el-button>
            <el-button type="primary" @click="handleSaveGroup">保存</el-button>
          </template>
        </el-dialog>

        <!-- 小组成员管理 -->
        <el-dialog v-model="membersDialog" :title="`${currentGroup?.name || ''} - 成员管理`" width="720px">
          <div class="member-toolbar">
            <span>小组成员（{{ members.length }}）</span>
            <div class="add-member">
              <el-select v-model="newMemberId" placeholder="从班内学生中选择" filterable clearable style="width: 240px">
                <el-option v-for="s in availableMembers" :key="s.id"
                           :label="displayName(s)" :value="s.id" />
              </el-select>
              <el-button type="primary" :disabled="!newMemberId" @click="handleAddMember">
                添加
              </el-button>
            </div>
          </div>
          <el-table :data="members" stripe border size="small">
            <el-table-column prop="username" label="账号" width="140" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="将该学生移出小组？" @confirm="handleRemoveMember(row.id)">
                  <template #reference>
                    <el-button type="danger" text size="small">移出</el-button>
                  </template>
                </el-popconfirm>
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
import {
  getClasses, addClass, updateClass, deleteClass,
  getClassStudents, addClassMember, removeClassMember,
  getGroups, addGroup, updateGroup, deleteGroup,
  getGroupMembers, addGroupMember, removeGroupMember,
  getStudentCandidates, getAllStudents,
  getMaterials, addMaterial, updateMaterial, deleteMaterial
} from '@/api/teaching'

const activeTab = ref('clazz')

const classes = ref([])
const groups = ref([])
const groupClazzId = ref(null)

function displayName(u) {
  return u?.realName ? u.realName : u?.username || ''
}

async function fetchClasses() {
  const res = await getClasses()
  if (res.code === 200) classes.value = res.data || []
}

async function fetchGroups() {
  if (!groupClazzId.value) {
    groups.value = []
    return
  }
  const res = await getGroups(groupClazzId.value)
  if (res.code === 200) groups.value = res.data || []
}

// ---------- 班级 ----------
const clazzDialog = ref(false)
const clazzForm = reactive({ id: null, name: '', grade: '', school: '' })

function openClazzDialog(row) {
  if (row) {
    Object.assign(clazzForm, { id: row.id, name: row.name, grade: row.grade, school: row.school })
  } else {
    Object.assign(clazzForm, { id: null, name: '', grade: '', school: '' })
  }
  clazzDialog.value = true
}

async function handleSaveClazz() {
  if (!clazzForm.name) {
    ElMessage.warning('请填写班级名称')
    return
  }
  if (clazzForm.id) {
    await updateClass(clazzForm)
  } else {
    await addClass(clazzForm)
  }
  ElMessage.success('保存成功')
  clazzDialog.value = false
  fetchClasses()
}

async function handleDeleteClazz(id) {
  await deleteClass(id)
  ElMessage.success('已删除')
  fetchClasses()
}

// ---------- 班级学生 ----------
const studentsDialog = ref(false)
const currentClazz = ref(null)
const students = ref([])
const candidates = ref([])
const newStudentId = ref(null)

async function openStudents(row) {
  currentClazz.value = row
  studentsDialog.value = true
  await Promise.all([fetchClassStudents(row.id), fetchCandidates(row.id)])
}

async function fetchClassStudents(clazzId) {
  const res = await getClassStudents(clazzId)
  if (res.code === 200) students.value = res.data || []
}

async function fetchCandidates(clazzId) {
  const res = await getStudentCandidates(clazzId)
  if (res.code === 200) candidates.value = res.data || []
}

async function handleAddStudent() {
  if (!newStudentId.value) return
  await addClassMember(currentClazz.value.id, newStudentId.value)
  ElMessage.success('已加入班级')
  newStudentId.value = null
  fetchClassStudents(currentClazz.value.id)
  fetchCandidates(currentClazz.value.id)
  fetchClasses()
}

async function handleRemoveStudent(userId) {
  await removeClassMember(currentClazz.value.id, userId)
  ElMessage.success('已移出')
  fetchClassStudents(currentClazz.value.id)
  fetchCandidates(currentClazz.value.id)
  fetchClasses()
}

// ---------- 班级学习资料 ----------
const materialsDialog = ref(false)
const materials = ref([])
const materialForm = reactive({ id: null, title: '', content: '' })

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : ''
}

async function openMaterials(row) {
  currentClazz.value = row
  materialsDialog.value = true
  resetMaterialForm()
  await fetchMaterials(row.id)
}

async function fetchMaterials(clazzId) {
  const res = await getMaterials(clazzId)
  if (res.code === 200) materials.value = res.data || []
}

function resetMaterialForm() {
  Object.assign(materialForm, { id: null, title: '', content: '' })
}

function editMaterial(row) {
  Object.assign(materialForm, { id: row.id, title: row.title, content: row.content || '' })
}

async function handleSaveMaterial() {
  if (!currentClazz.value) return
  if (!materialForm.title.trim()) {
    ElMessage.warning('请填写资料标题')
    return
  }
  if (materialForm.id) {
    await updateMaterial({ ...materialForm })
    ElMessage.success('资料已更新')
  } else {
    await addMaterial({
      ...materialForm,
      clazzId: currentClazz.value.id
    })
    ElMessage.success('资料已发布')
  }
  resetMaterialForm()
  fetchMaterials(currentClazz.value.id)
}

async function handleDeleteMaterial(id) {
  await deleteMaterial(id)
  ElMessage.success('资料已删除')
  resetMaterialForm()
  fetchMaterials(currentClazz.value.id)
}

// ---------- 小组 ----------
const groupDialog = ref(false)
const groupForm = reactive({ id: null, name: '', clazzId: null, leaderId: null })
const groupLeaders = ref([])

function openGroupDialog(row) {
  if (row) {
    Object.assign(groupForm, {
      id: row.id, name: row.name, clazzId: row.clazzId, leaderId: row.leaderId
    })
  } else {
    Object.assign(groupForm, { id: null, name: '', clazzId: groupClazzId.value, leaderId: null })
  }
  groupDialog.value = true
  loadLeaders(groupForm.clazzId)
}

async function loadLeaders(clazzId) {
  if (!clazzId) {
    groupLeaders.value = []
    return
  }
  const res = await getClassStudents(clazzId)
  if (res.code === 200) groupLeaders.value = res.data || []
}

async function handleSaveGroup() {
  if (!groupForm.name) {
    ElMessage.warning('请填写小组名称')
    return
  }
  if (!groupForm.clazzId) {
    ElMessage.warning('请选择所属班级')
    return
  }
  if (groupForm.id) {
    await updateGroup(groupForm)
  } else {
    await addGroup(groupForm)
  }
  ElMessage.success('保存成功')
  groupDialog.value = false
  fetchGroups()
}

async function handleDeleteGroup(id) {
  await deleteGroup(id)
  ElMessage.success('已删除')
  fetchGroups()
}

// ---------- 小组名成员 ----------
const membersDialog = ref(false)
const currentGroup = ref(null)
const members = ref([])
const availableMembers = ref([])
const newMemberId = ref(null)

async function openGroupMembers(row) {
  currentGroup.value = row
  membersDialog.value = true
  await fetchGroupMembers(row.id)
}

async function fetchGroupMembers(groupId) {
  const res = await getGroupMembers(groupId)
  if (res.code === 200) {
    members.value = res.data.members || []
    availableMembers.value = res.data.available || []
  }
}

async function handleAddMember() {
  if (!newMemberId.value) return
  await addGroupMember(currentGroup.value.id, newMemberId.value)
  ElMessage.success('已添加')
  newMemberId.value = null
  fetchGroupMembers(currentGroup.value.id)
  fetchGroups()
}

async function handleRemoveMember(userId) {
  await removeGroupMember(currentGroup.value.id, userId)
  ElMessage.success('已移出')
  fetchGroupMembers(currentGroup.value.id)
  fetchGroups()
}

onMounted(async () => {
  await fetchClasses()
  fetchGroups()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.member-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);

  .add-member {
    display: flex;
    gap: 8px;
  }
}
</style>
