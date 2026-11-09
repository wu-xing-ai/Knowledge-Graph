import { describe, it, expect, beforeEach, vi } from 'vitest'
import router, { resetAppStatusCache } from '../index'
import { getAppStatus } from '@/api/app'

// 演示模式检测接口：默认返回「要求登录」，演示模式用例单独覆写返回值
vi.mock('@/api/app', () => ({ getAppStatus: vi.fn() }))

// 路由守卫测试只关心导航逻辑，用轻量桩组件替换所有懒加载视图，
// 避免在完整测试套件下因加载重型组件（Element Plus / G6）导致超时。
vi.mock('@/views/Login.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/components/layout/AppLayout.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/Dashboard.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/GraphView.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/QA.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/Learning.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/teacher/KnowledgeBase.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/teacher/GraphConfig.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/teacher/ExerciseManage.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/teacher/SystemAdmin.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/student/StudyCenter.vue', () => ({ default: { template: '<div />' } }))

describe('router guards', () => {
  beforeEach(async () => {
    localStorage.clear()
    getAppStatus.mockResolvedValue({ data: { authRequired: true } })
    resetAppStatusCache()
    await router.push('/login')
    await router.isReady()
  })

  it('redirects to login when no token', async () => {
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows login page without token', async () => {
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows access to dashboard with token', async () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('user', JSON.stringify({ role: 'STUDENT' }))
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('redirects student away from teacher pages', async () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('user', JSON.stringify({ role: 'STUDENT' }))
    await router.push('/teacher/knowledge-base')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('allows teacher to access teacher pages', async () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('user', JSON.stringify({ role: 'TEACHER' }))
    await router.push('/teacher/knowledge-base')
    expect(router.currentRoute.value.path).toBe('/teacher/knowledge-base')
  })

  it('auto demo-login when auth disabled', async () => {
    getAppStatus.mockResolvedValue({
      data: { authRequired: false, token: 'demo-token', role: 'ADMIN', username: 'admin', realName: '管理员', userId: 1 }
    })
    resetAppStatusCache()
    await router.push('/dashboard')
    expect(localStorage.getItem('token')).toBe('demo-token')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('skips login page when demo mode active', async () => {
    getAppStatus.mockResolvedValue({
      data: { authRequired: false, token: 'demo-token', role: 'ADMIN', username: 'admin', realName: '管理员', userId: 1 }
    })
    resetAppStatusCache()
    // 先携带演示 token 进入 dashboard，再访问 /login
    localStorage.setItem('token', 'demo-token')
    localStorage.setItem('user', JSON.stringify({ role: 'ADMIN' }))
    await router.push('/dashboard')
    expect(router.currentRoute.value.path).toBe('/dashboard')
    // 演示模式下访问登录页应被弹回工作台
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })
})
