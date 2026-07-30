<template>
  <div class="sidebar">
    <div class="logo">
      <el-icon size="24"><Connection /></el-icon>
      <span>知联汇学</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      router
      background-color="#001529"
      text-color="#ffffffb3"
      active-text-color="#fff"
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <span>平台首页</span>
      </el-menu-item>
      <el-menu-item index="/graph">
        <el-icon><Share /></el-icon>
        <span>知识图谱</span>
      </el-menu-item>
      <el-menu-item index="/qa">
        <el-icon><ChatDotRound /></el-icon>
        <span>智能问答</span>
      </el-menu-item>
      <el-menu-item index="/learning">
        <el-icon><Reading /></el-icon>
        <span>学习辅助</span>
      </el-menu-item>

      <template v-if="isTeacher">
        <el-divider style="border-color: #ffffff30; margin: 12px 16px;" />
        <el-menu-item index="/teacher/knowledge-base">
          <el-icon><Notebook /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
        <el-menu-item index="/teacher/graph-config">
          <el-icon><Setting /></el-icon>
          <span>图谱配置</span>
        </el-menu-item>
        <el-menu-item index="/teacher/exercise">
          <el-icon><Edit /></el-icon>
          <span>习题管理</span>
        </el-menu-item>
        <el-menu-item index="/teacher/admin">
          <el-icon><UserFilled /></el-icon>
          <span>系统管理</span>
        </el-menu-item>
      </template>

      <template v-if="isStudent">
        <el-divider style="border-color: #ffffff30; margin: 12px 16px;" />
        <el-menu-item index="/student/center">
          <el-icon><User /></el-icon>
          <span>学习中心</span>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const isTeacher = computed(() => userStore.isTeacher())
const isStudent = computed(() => userStore.user?.role === 'STUDENT')
</script>

<style scoped lang="scss">
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: #001529;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid #ffffff15;
}

.el-menu {
  border-right: none;
  flex: 1;

  .el-menu-item {
    &:hover {
      background: #ffffff15 !important;
    }
    &.is-active {
      background: var(--primary) !important;
    }
  }
}
</style>
