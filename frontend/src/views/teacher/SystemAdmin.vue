<template>
  <div class="page-container">
    <div class="page-header">
      <h2>系统管理（管理员）</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex-wrap:wrap;gap:8px;">
          <div style="display:flex;align-items:center;gap:12px;">
            <el-radio-group v-model="filterStatus" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="pending">待审核（{{ pendingCount }}）</el-radio-button>
            </el-radio-group>
            <span style="color:var(--text-secondary);font-size:13px;">共 {{ users.length }} 个账号</span>
          </div>
          <el-upload :show-file-list="false" :before-upload="handleBatchImport" accept=".xlsx,.xls">
            <el-button><el-icon><Upload /></el-icon>批量导入学生</el-button>
          </el-upload>
          <el-button type="primary" @click="openAddDialog"><el-icon><Plus /></el-icon>新增账号</el-button>
        </div>

        <el-table :data="filteredUsers" stripe border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="账号" width="130" />
          <el-table-column prop="realName" label="姓名" width="110" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-select
                v-model="row.role"
                size="small"
                :disabled="row.username === 'admin'"
                @change="onRoleChange(row)"
                style="width:100%;"
              >
                <el-option label="管理员" value="ADMIN" />
                <el-option label="教师" value="TEACHER" />
                <el-option label="学生" value="STUDENT" />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.status === 2" type="warning" size="small">待审核</el-tag>
              <el-tag v-else :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="170" />
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 2">
                <el-button type="success" text @click="approveUser(row)">通过</el-button>
                <el-popconfirm title="拒绝该注册申请并删除账号？" @confirm="rejectUser(row)">
                  <template #reference><el-button type="danger" text>拒绝</el-button></template>
                </el-popconfirm>
              </template>
              <el-button type="primary" text @click="openResetDialog(row)">重置密码</el-button>
              <el-popconfirm v-if="row.username !== 'admin'" title="确定删除该账号？" @confirm="handleDelete(row)">
                <template #reference><el-button type="danger" text>删除</el-button></template>
              </el-popconfirm>
              <el-switch
                v-if="row.status !== 2"
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                @change="toggleStatus(row)"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <el-table :data="logs" stripe border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
        <el-empty v-if="!logs.length" description="暂无日志记录" />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增账号 -->
    <el-dialog v-model="addVisible" title="新增账号" width="480px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="账号" required>
          <el-input v-model="addForm.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="addForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="addForm.password" type="password" placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="addForm.role" style="width:100%;">
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <p style="margin-bottom:12px;color:var(--text-secondary);font-size:13px;">
        重置用户「{{ resetTarget?.username }}」的密码（BCrypt 加密不可逆，无法查看明文，只能重置）
      </p>
      <el-input v-model="resetPassword" type="password" placeholder="新密码（至少 6 位）" show-password />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getAdminUsers, adminAddUser, adminResetPassword, adminUpdateStatus,
  adminUpdateRole, adminDeleteUser, getAdminLogs
} from '@/api/admin'
import { batchImportUsers } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('users')
const users = ref([])
const logs = ref([])
const filterStatus = ref('all')

const addVisible = ref(false)
const adding = ref(false)
const addForm = reactive({ username: '', realName: '', password: '', role: 'TEACHER' })

const resetVisible = ref(false)
const resetting = ref(false)
const resetTarget = ref(null)
const resetPassword = ref('')

const pendingCount = computed(() => users.value.filter(u => u.status === 2).length)
const filteredUsers = computed(() =>
  filterStatus.value === 'pending' ? users.value.filter(u => u.status === 2) : users.value
)

async function fetchUsers() {
  const res = await getAdminUsers()
  if (res.code === 200) users.value = res.data || []
}

async function fetchLogs() {
  const res = await getAdminLogs()
  if (res.code === 200) logs.value = res.data || []
}

async function toggleStatus(row) {
  try {
    const res = await adminUpdateStatus(row.id, row.status)
    if (res.code === 200) ElMessage.success('状态已更新')
    else { ElMessage.error(res.message || '更新失败'); fetchUsers() }
  } catch (e) {
    ElMessage.error('更新失败'); fetchUsers()
  }
}

// 审核通过
async function approveUser(row) {
  try {
    const res = await adminUpdateStatus(row.id, 1)
    if (res.code === 200) { ElMessage.success('已通过，教师账号可登录'); fetchUsers() }
    else ElMessage.error(res.message || '操作失败')
  } catch (e) { ElMessage.error('操作失败') }
}

// 拒绝注册申请（删除账号）
async function rejectUser(row) {
  try {
    const res = await adminDeleteUser(row.id)
    if (res.code === 200) { ElMessage.success('已拒绝并删除该账号'); fetchUsers() }
    else ElMessage.error(res.message || '操作失败')
  } catch (e) { ElMessage.error('操作失败') }
}

// 修改身份
async function onRoleChange(row) {
  const oldRole = row.role
  try {
    const res = await adminUpdateRole(row.id, row.role)
    if (res.code === 200) ElMessage.success(`身份已改为${roleLabel(row.role)}`)
    else { ElMessage.error(res.message || '修改失败'); fetchUsers() }
  } catch (e) {
    ElMessage.error('修改失败'); fetchUsers()
  }
}

async function handleDelete(row) {
  try {
    const res = await adminDeleteUser(row.id)
    if (res.code === 200) { ElMessage.success('账号已删除'); fetchUsers() }
    else ElMessage.error(res.message || '删除失败')
  } catch (e) { ElMessage.error('删除失败') }
}

function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : role === 'TEACHER' ? '教师' : '学生'
}

// 批量导入学生账号：上传 Excel（用户名/姓名/初始密码/班级名）
async function handleBatchImport(file) {
  try {
    const res = await batchImportUsers(file)
    if (res.code === 200) {
      const data = res.data || {}
      const lines = [`成功导入 ${data.successCount ?? 0} 个学生账号`, `失败 ${data.failCount ?? 0} 条`]
      if (Array.isArray(data.failures) && data.failures.length) {
        lines.push('失败明细：' + data.failures.map(f => `${f.username || ''}(第${f.row}行)：${f.reason}`).join('；'))
      }
      if (Array.isArray(data.notes) && data.notes.length) {
        lines.push('备注：' + data.notes.map(n => n.reason).join('；'))
      }
      await ElMessageBox.alert(lines.join('\n'), '批量导入结果')
      fetchUsers()
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败')
  }
  return false
}

function openAddDialog() {
  Object.assign(addForm, { username: '', realName: '', password: '', role: 'TEACHER' })
  addVisible.value = true
}

async function handleAdd() {
  if (!addForm.username || !addForm.password || addForm.password.length < 6) {
    ElMessage.warning('请填写账号和至少 6 位的密码')
    return
  }
  adding.value = true
  try {
    const res = await adminAddUser({ ...addForm })
    if (res.code === 200) {
      ElMessage.success('账号已创建')
      addVisible.value = false
      fetchUsers()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    adding.value = false
  }
}

function openResetDialog(row) {
  resetTarget.value = row
  resetPassword.value = ''
  resetVisible.value = true
}

async function handleReset() {
  if (!resetPassword.value || resetPassword.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resetting.value = true
  try {
    const res = await adminResetPassword(resetTarget.value.id, resetPassword.value)
    if (res.code === 200) {
      ElMessage.success('密码已重置')
      resetVisible.value = false
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch (e) {
    ElMessage.error('重置失败')
  } finally {
    resetting.value = false
  }
}


onMounted(() => {
  fetchUsers()
  fetchLogs()
})
</script>
