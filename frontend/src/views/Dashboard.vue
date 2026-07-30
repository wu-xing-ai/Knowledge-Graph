<template>
  <div class="page-container">
    <div class="welcome">
      <h2>欢迎使用知联汇学，{{ userStore.user?.realName || userStore.user?.username }}！</h2>
      <p class="sub">交叉学科知识图谱平台</p>
    </div>

    <!-- 教师视角 -->
    <template v-if="userStore.isTeacher()">
      <h3 style="margin: 20px 0 12px; color: #606266;">快捷入口</h3>
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
      <h3 style="margin: 20px 0 12px; color: #606266;">学习入口</h3>
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
    <h3 style="margin: 30px 0 12px; color: #606266;">热门交叉学科专题</h3>
    <div class="card-grid">
      <el-card v-for="topic in crossTopics" :key="topic.name" shadow="hover" class="topic-card">
        <template #header>
          <div style="display:flex;align-items:center;gap:8px;">
            <el-tag :color="topic.color" effect="dark" style="color:#fff;border:none;">{{ topic.tag }}</el-tag>
            <span style="font-weight:600;">{{ topic.name }}</span>
          </div>
        </template>
        <p style="color:#606266;font-size:13px;line-height:1.8;">{{ topic.desc }}</p>
        <el-button type="primary" link @click="$router.push('/graph')">查看图谱</el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const teacherCards = [
  { title: '知识库管理', desc: '管理交叉学科知识点与关联关系', path: '/teacher/knowledge-base', icon: 'Notebook', color: '#4A90D9' },
  { title: '知识图谱', desc: '可视化浏览跨学科知识关联', path: '/graph', icon: 'Share', color: '#6C5CE7' },
  { title: '智能问答', desc: '跨学科问题智能解答', path: '/qa', icon: 'ChatDotRound', color: '#F5A623' },
  { title: '习题管理', desc: '管理跨学科习题与考点', path: '/teacher/exercise', icon: 'Edit', color: '#7ED321' },
  { title: '图谱配置', desc: '自定义图谱布局与配色', path: '/teacher/graph-config', icon: 'Setting', color: '#9B59B6' },
  { title: '系统管理', desc: '用户权限与系统日志', path: '/teacher/admin', icon: 'UserFilled', color: '#50E3C2' }
]

const studentCards = [
  { title: '知识图谱', desc: '探索跨学科知识关联', path: '/graph', icon: 'Share', color: '#6C5CE7' },
  { title: '智能问答', desc: '跨学科问题智能解答', path: '/qa', icon: 'ChatDotRound', color: '#F5A623' },
  { title: '学习辅助', desc: '知识点溯源与刷题', path: '/learning', icon: 'Reading', color: '#7ED321' },
  { title: '学习中心', desc: '个人学习记录与笔记', path: '/student/center', icon: 'User', color: '#4A90D9' }
]

const crossTopics = [
  { name: '航天中的物理与化学', tag: '航天交叉', color: '#9B59B6', desc: '探索火箭发射、卫星运行中的物理力学原理与化学燃料反应，理解航天科技的跨学科基础。' },
  { name: '水污染治理中的生物与化学', tag: '环境交叉', color: '#50E3C2', desc: '了解生物降解与化学氧化在水污染治理中的协同作用，掌握环保领域的跨学科知识。' },
  { name: '环境科学中的物理与生物', tag: '环境交叉', color: '#0984E3', desc: '分析物理过滤与生物净化在环境治理中的配合应用，理解环境科学的多学科交叉。' }
]
</script>

<style scoped lang="scss">
.welcome {
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;

  h2 { font-size: 22px; }
  .sub { margin-top: 4px; opacity: 0.85; font-size: 14px; }
}

.topic-card {
  cursor: pointer;
  transition: all 0.3s;
  &:hover { transform: translateY(-2px); }
}
</style>
