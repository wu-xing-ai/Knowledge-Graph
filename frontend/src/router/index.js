import { createRouter, createWebHistory } from 'vue-router'

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

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.meta.role) {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const role = user.role || ''
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

