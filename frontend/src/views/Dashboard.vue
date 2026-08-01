<template>
  <div class="page-container">
    <div class="welcome">
      <div class="welcome-content">
        <h2>欢迎回来，{{ userStore.user?.realName || userStore.user?.username }}！</h2>
        <p class="sub">探索交叉学科知识网络 · AI 驱动学习</p>
      </div>
      <div class="welcome-stats">
        <div class="stat">
          <b>5</b><span>大学科</span>
        </div>
        <div class="stat">
          <b>∞</b><span>知识关联</span>
        </div>
        <div class="stat">
          <b>AI</b><span>智能问答</span>
        </div>
      </div>
    </div>

    <!-- 教师视角 -->
    <template v-if="userStore.isTeacher()">
      <h3 class="section-title">快捷入口</h3>
      <div class="card-grid">
        <div class="stat-card" v-for="item in teacherCards" :key="item.title" @click="$router.push(item.path)">
          <div class="icon" :style="{ color: item.color }">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <div class="title">{{ item.title }}</div>
          <div class="desc">{{ item.desc }}</div>
        </div>
      </div>
    </template>

    <!-- 学生视角 -->
    <template v-else>
      <h3 class="section-title">学习入口</h3>
      <div class="card-grid">
        <div class="stat-card" v-for="item in studentCards" :key="item.title" @click="$router.push(item.path)">
          <div class="icon" :style="{ color: item.color }">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <div class="title">{{ item.title }}</div>
          <div class="desc">{{ item.desc }}</div>
        </div>
      </div>
    </template>

    <!-- 交叉学科专题推荐 -->
    <h3 class="section-title">热门交叉学科专题</h3>
    <div class="card-grid">
      <el-card v-for="topic in crossTopics" :key="topic.name" shadow="hover" class="topic-card glass-card">
        <template #header>
          <div style="display:flex;align-items:center;gap:8px;">
            <el-tag :color="topic.color" effect="dark" style="color:#fff;border:none;">{{ topic.tag }}</el-tag>
            <span style="font-weight:600;color:var(--text-primary);">{{ topic.name }}</span>
          </div>
        </template>
        <p style="color:var(--text-secondary);font-size:13px;line-height:1.8;">{{ topic.desc }}</p>
        <el-button type="primary" link @click="$router.push('/graph')">查看图谱 →</el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const teacherCards = [
  { title: '知识库管理', desc: '管理交叉学科知识点与关联关系', path: '/teacher/knowledge-base', icon: 'Notebook', color: '#1783FF' },
  { title: '知识图谱', desc: '可视化浏览跨学科知识关联', path: '/graph', icon: 'Share', color: '#4F46E5' },
  { title: '智能问答', desc: '跨学科问题智能解答', path: '/qa', icon: 'ChatDotRound', color: '#D97706' },
  { title: '习题管理', desc: '管理跨学科习题与考点', path: '/teacher/exercise', icon: 'Edit', color: '#16A34A' },
  { title: '图谱配置', desc: '自定义图谱布局与配色', path: '/teacher/graph-config', icon: 'Setting', color: '#7C3AED' },
  { title: '系统管理', desc: '用户权限与系统日志', path: '/teacher/admin', icon: 'UserFilled', color: '#0D9488' }
]

const studentCards = [
  { title: '知识图谱', desc: '探索跨学科知识关联', path: '/graph', icon: 'Share', color: '#4F46E5' },
  { title: '智能问答', desc: '跨学科问题智能解答', path: '/qa', icon: 'ChatDotRound', color: '#D97706' },
  { title: '学习辅助', desc: '知识点溯源与刷题', path: '/learning', icon: 'Reading', color: '#16A34A' },
  { title: '学习中心', desc: '个人学习记录与笔记', path: '/student/center', icon: 'User', color: '#1783FF' }
]

const crossTopics = [
  { name: '航天中的物理与化学', tag: '航天交叉', color: '#8859A3', desc: '探索火箭发射、卫星运行中的物理力学原理与化学燃料反应，理解航天科技的跨学科基础。' },
  { name: '水污染治理中的生物与化学', tag: '环境交叉', color: '#39B5A8', desc: '了解生物降解与化学氧化在水污染治理中的协同作用，掌握环保领域的跨学科知识。' },
  { name: '环境科学中的物理与生物', tag: '环境交叉', color: '#5B9BD5', desc: '分析物理过滤与生物净化在环境治理中的配合应用，理解环境科学的多学科交叉。' }
]
</script>

<style scoped lang="scss">
.welcome {
  padding: 24px 28px;
  border-radius: 10px;
  border: 1px solid var(--border);
  border-left: 4px solid var(--primary);
  background: var(--bg-panel);
  display: flex;
  justify-content: space-between;
  align-items: center;

  .welcome-content {
    h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); letter-spacing: 0.5px; }
    .sub { margin-top: 8px; color: var(--text-secondary); font-size: 14px; letter-spacing: 1px; }
  }

  .welcome-stats {
    display: flex;
    gap: 26px;

    .stat {
      display: flex;
      flex-direction: column;
      align-items: center;
      b { font-size: 24px; font-weight: 700; color: var(--primary); }
      span { margin-top: 2px; font-size: 11px; color: var(--text-muted); letter-spacing: 1px; }
    }
  }
}

.topic-card {
  cursor: pointer;
}
</style>
