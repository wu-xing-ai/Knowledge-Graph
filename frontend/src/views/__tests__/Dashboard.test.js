import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Dashboard from '../Dashboard.vue'
import { useUserStore } from '@/store/user'

const stubs = {
  'el-card': {
    template: '<div><slot name="header" /><slot /></div>'
  },
  'el-tag': { template: '<span><slot /></span>' },
  'el-button': { template: '<button><slot /></button>' },
  'el-icon': { template: '<span><slot /></span>' }
}

describe('Dashboard.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('shows teacher quick entries for teacher role', () => {
    const store = useUserStore()
    store.setUser({ username: 'admin', realName: '管理员', role: 'TEACHER' })

    const wrapper = mount(Dashboard, { global: { stubs } })

    expect(wrapper.text()).toContain('知识库管理')
    expect(wrapper.text()).toContain('习题管理')
    expect(wrapper.text()).toContain('系统管理')
  })

  it('shows student learning entries for student role', () => {
    const store = useUserStore()
    store.setUser({ username: 'student', realName: '学生', role: 'STUDENT' })

    const wrapper = mount(Dashboard, { global: { stubs } })

    expect(wrapper.text()).toContain('知识图谱')
    expect(wrapper.text()).toContain('智能问答')
    expect(wrapper.text()).toContain('学习中心')
    expect(wrapper.text()).not.toContain('系统管理')
  })

  it('shows cross subject topics for all users', () => {
    const store = useUserStore()
    store.setUser({ username: 'user', realName: '用户', role: 'STUDENT' })

    const wrapper = mount(Dashboard, { global: { stubs } })

    expect(wrapper.text()).toContain('航天中的物理与化学')
    expect(wrapper.text()).toContain('水污染治理中的生物与化学')
  })
})
