import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getAppStatus } from '@/api/app'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'graph',
        name: 'GraphView',
        component: () => import('@/views/GraphView.vue')
      },
      {
        path: 'qa',
        name: 'QA',
        component: () => import('@/views/QA.vue')
      },
      {
        path: 'learning',
        name: 'Learning',
        component: () => import('@/views/Learning.vue')
      },
      {
        path: 'teacher/knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/teacher/KnowledgeBase.vue'),
        meta: { role: 'TEACHER' }
      },
      {
        path: 'teacher/graph-config',
        name: 'GraphConfig',
        component: () => import('@/views/teacher/GraphConfig.vue'),
        meta: { role: 'TEACHER' }
      },
      {
        path: 'teacher/exercise',
        name: 'ExerciseManage',
        component: () => import('@/views/teacher/ExerciseManage.vue'),
        meta: { role: 'TEACHER' }
      },
      {
        path: 'teacher/classes',
        name: 'ClassManage',
        component: () => import('@/views/teacher/ClassManage.vue'),
        meta: { role: 'TEACHER' }
      },
      {
        path: 'teacher/tasks',
        name: 'TaskAssign',
        component: () => import('@/views/teacher/TaskAssign.vue'),
        meta: { role: 'TEACHER' }
      },
      {
        path: 'admin/system',
        name: 'SystemAdmin',
        component: () => import('@/views/teacher/SystemAdmin.vue'),
        meta: { role: 'ADMIN' }
      },
      {
        path: 'student/center',
        name: 'StudyCenter',
        component: () => import('@/views/student/StudyCenter.vue'),
        meta: { role: 'STUDENT' }
      },
      {
        path: 'student/tasks',
        name: 'MyTasks',
        component: () => import('@/views/student/MyTasks.vue'),
        meta: { role: 'STUDENT' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 应用状态缓存：一次会话只加载一次；null/undefined 表示请求失败（按正常登录模式处理）
let cachedStatus
let statusLoaded = false
function loadAppStatus() {
  if (statusLoaded) return Promise.resolve(cachedStatus)
  return getAppStatus()
    .then(res => {
      cachedStatus = res.data || null
      statusLoaded = true
      return cachedStatus
    })
    .catch(() => {
      cachedStatus = null
      statusLoaded = true
      return null
    })
}

/** 仅测试使用：重置应用状态缓存 */
export function resetAppStatusCache() {
  cachedStatus = undefined
  statusLoaded = false
}

// 读取当前登录态；测试环境没有激活 Pinia 时回退 localStorage
function readAuth() {
  try {
    const userStore = useUserStore()
    return {
      token: userStore.token || '',
      role: userStore.user?.role || '',
      store: userStore
    }
  } catch (e) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    return {
      token: localStorage.getItem('token') || '',
      role: user?.role || '',
      store: null
    }
  }
}

// 演示模式（后端 AUTH_ENABLED=false）：无 token 时自动以演示账号登录
async function tryDemoLogin() {
  const status = await loadAppStatus()
  if (status && status.authRequired === false && status.token) {
    const payload = {
      userId: status.userId,
      username: status.username,
      realName: status.realName,
      role: status.role
    }
    const { store } = readAuth()
    if (store) {
      store.setToken(status.token)
      store.setUser(payload)
    } else {
      localStorage.setItem('token', status.token)
      localStorage.setItem('user', JSON.stringify(payload))
    }
    return true
  }
  return false
}

router.beforeEach(async (to, from, next) => {
  let { token, role } = readAuth()
  if (!token) {
    await tryDemoLogin()
    ;({ token, role } = readAuth())
  }
  if (!token && to.path !== '/login') {
    next('/login')
    return
  }
  // 演示模式下不再停留登录页（退出登录会被自动重新登入）
  if (to.path === '/login' && token) {
    const status = await loadAppStatus()
    if (status && status.authRequired === false) {
      next('/dashboard')
      return
    }
  }
  if (to.meta.role) {
    // 管理员可访问所有教师页面；系统管理仅管理员
    const allowed = to.meta.role === 'ADMIN'
      ? role === 'ADMIN'
      : role === to.meta.role || role === 'ADMIN'
    if (!allowed) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router

