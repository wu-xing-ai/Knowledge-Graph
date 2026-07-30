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
        path: 'teacher/admin',
        name: 'SystemAdmin',
        component: () => import('@/views/teacher/SystemAdmin.vue'),
        meta: { role: 'TEACHER' }
      },
      {
        path: 'student/center',
        name: 'StudyCenter',
        component: () => import('@/views/student/StudyCenter.vue'),
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
    if (user.role !== to.meta.role) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router
