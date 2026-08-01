<template>
  <div class="sidebar" ref="sidebarEl">
    <div class="logo">
      <div class="logo-mark">
        <el-icon size="18"><Connection /></el-icon>
      </div>
      <div class="logo-text">
        <span class="name">知联汇学</span>
        <span class="sub">交叉学科知识图谱</span>
      </div>
    </div>

    <el-menu :default-active="activeMenu" router class="side-menu">
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
        <div class="menu-group-label">教学管理</div>
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
        <div class="menu-group-label">个人学习</div>
        <el-menu-item index="/student/center">
          <el-icon><User /></el-icon>
          <span>学习中心</span>
        </el-menu-item>
      </template>
    </el-menu>

    <div class="sidebar-footer">
      <p>Knowledge Network</p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { createLiquidGlass } from '@/utils/liquidGlass/liquidGlass'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const isTeacher = computed(() => userStore.isTeacher())
const isStudent = computed(() => userStore.user?.role === 'STUDENT')

// 侧栏静态液态玻璃面板（Skill 引擎 createLiquidGlass：Snell 折射 + 物理高光 + 色散）
const sidebarEl = ref(null)
let glass = null
onMounted(() => {
  if (sidebarEl.value) glass = createLiquidGlass(sidebarEl.value)
})
onBeforeUnmount(() => {
  if (glass) glass.destroy()
})
</script>

<style scoped lang="scss">
/* 液态玻璃侧栏（结构参考 Liquid Glass Studio：着色层 + 高光描边 + 折射模糊） */
.sidebar {
  position: relative;
  isolation: isolate;
  width: var(--sidebar-width);
  height: calc(100vh - 24px);
  margin: 12px 0 12px 12px;
  border-radius: 18px; /* 引擎按此圆角对齐折射 SDF 与高光 */
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  z-index: 10;
  animation: lg-sidebar-in 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes lg-sidebar-in {
  from { transform: translateX(-18px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(31, 35, 41, 0.06);
  flex-shrink: 0;

  .logo-mark {
    width: 34px;
    height: 34px;
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #1F2329;
    color: #fff;
    flex-shrink: 0;
  }

  .logo-text {
    display: flex;
    flex-direction: column;
    .name {
      font-size: 16px;
      font-weight: 700;
      letter-spacing: 1px;
      color: var(--text-primary);
    }
    .sub {
      font-size: 10px;
      color: var(--text-muted);
      letter-spacing: 1.5px;
      margin-top: 2px;
    }
  }
}

.menu-group-label {
  padding: 16px 20px 6px;
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 2px;
}

.side-menu {
  border-right: none;
  flex: 1;
  background: transparent;
  overflow-y: auto;

  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--text-secondary);
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.4);
  --el-menu-active-color: var(--primary);

  .el-menu-item {
    height: 42px;
    margin: 2px 10px;
    border-radius: 10px;
    border: 1px solid transparent;
    /* 弹簧缓动，参考参考站拖拽/吸附的 spring 物理感 */
    transition:
      transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1),
      background-color 0.15s ease,
      color 0.15s ease,
      box-shadow 0.25s ease;

    &:hover {
      color: var(--text-primary);
      transform: translateX(3px);
    }

    /* 选中项 =  mini 液态玻璃块，弹入动画 */
    &.is-active {
      background: rgba(255, 255, 255, 0.72) !important;
      color: var(--primary) !important;
      font-weight: 600;
      border-color: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(8px);
      box-shadow:
        inset 1px 1px 1px rgba(255, 255, 255, 0.85),
        inset -1px -1px 1px rgba(255, 255, 255, 0.2),
        0 4px 14px rgba(23, 131, 255, 0.12);
      animation: lg-spring-in 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
  }
}

@keyframes lg-spring-in {
  0% { transform: scale(0.94); }
  60% { transform: scale(1.02); }
  100% { transform: scale(1); }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(31, 35, 41, 0.06);

  p {
    font-size: 10px;
    color: var(--text-muted);
    letter-spacing: 3px;
    text-transform: uppercase;
  }
}
</style>
