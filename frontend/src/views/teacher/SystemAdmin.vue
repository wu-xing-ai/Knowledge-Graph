<template>
  <div class="page-container">
    <div class="page-header">
      <h2>系统权限与日志管理</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" stripe border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="role" label="角色" width="80">
            <template #default="{ row }">
              <el-tag :type="row.role === 'TEACHER' ? 'danger' : 'success'" size="small">
                {{ row.role === 'TEACHER' ? '教师' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="className" label="班级" width="100" />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-switch
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
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="module" label="模块" width="100" />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
        <el-empty v-if="!logs.length" description="暂无日志记录" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, updateUserStatus } from '@/api/user'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const activeTab = ref('users')
const users = ref([])
const logs = ref([])

async function fetchUsers() {
  const res = await getUserList()
  if (res.code === 200) users.value = res.data || []
}

async function fetchLogs() {
  const res = await request.get('/admin/logs')
  if (res.code === 200) logs.value = res.data || []
}

async function toggleStatus(row) {
  await updateUserStatus(row.id, row.status)
  ElMessage.success('状态已更新')
}

onMounted(() => {
  fetchUsers()
  fetchLogs()
})
</script>
