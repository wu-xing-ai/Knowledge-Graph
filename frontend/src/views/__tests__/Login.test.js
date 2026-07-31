import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Login from '../Login.vue'

const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock })
}))

vi.mock('@/api/user', () => ({
  login: vi.fn(),
  register: vi.fn()
}))

import { login, register } from '@/api/user'

const stubs = {
  'el-form': {
    template: '<form><slot /></form>',
    methods: { validate: vi.fn().mockResolvedValue(true) }
  },
  'el-form-item': { template: '<div><slot /></div>' },
  'el-input': { template: '<input />' },
  'el-button': { template: '<button><slot /></button>' },
  'el-tabs': { template: '<div><slot /></div>' },
  'el-tab-pane': { template: '<div><slot /></div>' },
  'el-radio-group': { template: '<div><slot /></div>' },
  'el-radio-button': { template: '<div><slot /></div>' },
  'el-icon': { template: '<span><slot /></span>' },
  Connection: { template: '<span />' }
}

describe('Login.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders login form by default', () => {
    const wrapper = mount(Login, { global: { stubs } })
    expect(wrapper.text()).toContain('知联汇学')
    expect(wrapper.text()).toContain('交叉学科知识图谱平台')
  })

  it('calls login api and navigates to dashboard on success', async () => {
    login.mockResolvedValue({ data: { token: 'token', userId: 1, username: 'admin', realName: '管理员', role: 'TEACHER' } })

    const wrapper = mount(Login, { global: { stubs } })
    wrapper.vm.loginForm.username = 'admin'
    wrapper.vm.loginForm.password = '123456'

    await wrapper.vm.handleLogin()

    expect(login).toHaveBeenCalledWith({ username: 'admin', password: '123456' })
    expect(pushMock).toHaveBeenCalledWith('/dashboard')
    expect(localStorage.getItem('token')).toBe('token')
  })

  it('calls register api on register submit', async () => {
    register.mockResolvedValue({})

    const wrapper = mount(Login, { global: { stubs } })
    wrapper.vm.registerForm.username = 'newuser'
    wrapper.vm.registerForm.password = 'password'
    wrapper.vm.registerForm.realName = '张三'
    wrapper.vm.registerForm.role = 'STUDENT'

    await wrapper.vm.handleRegister()

    expect(register).toHaveBeenCalled()
  })
})
