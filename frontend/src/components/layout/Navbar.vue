<template>
  <div class="navbar">
    <div class="left">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="right">
      <div class="system-badge">
        <span class="dot"></span>
        <span>知识网络在线</span>
      </div>
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <div class="avatar">{{ avatarText }}</div>
          <span class="name">{{ userStore.user?.realName || userStore.user?.username }}</span>
          <el-tag size="small" effect="dark" class="role-tag" :class="userStore.isTeacher() ? 'is-teacher' : 'is-student'">
            {{ userStore.isTeacher() ? '教师' : '学生' }}
          </el-tag>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const avatarText = computed(() => {
  const name = userStore.user?.realName || userStore.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.navbar {
  height: 56px;
  background: var(--bg-panel);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 9;
  flex-shrink: 0;
}

:deep(.el-breadcrumb__inner) { color: var(--text-secondary); }
:deep(.el-breadcrumb__separator) { color: var(--text-muted); }
:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: var(--text-primary); }

.right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.system-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #16A34A;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.15s;

  &:hover { background: var(--bg-hover); }

  .avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 14px;
  }

  .name {
    font-size: 14px;
    color: var(--text-primary);
  }

  .role-tag {
    border: none;
    &.is-teacher { background: #FDF3E3; color: #D97706; }
    &.is-student { background: #E6F6EC; color: #16A34A; }
  }
}
</style>
