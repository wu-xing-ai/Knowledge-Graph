import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('initializes with token and user from localStorage', () => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('user', JSON.stringify({ username: 'admin', role: 'TEACHER' }))

    setActivePinia(createPinia())
    const store = useUserStore()

    expect(store.token).toBe('test-token')
    expect(store.user).toEqual({ username: 'admin', role: 'TEACHER' })
  })

  it('setToken updates token and persists to localStorage', () => {
    const store = useUserStore()
    store.setToken('new-token')

    expect(store.token).toBe('new-token')
    expect(localStorage.getItem('token')).toBe('new-token')
  })

  it('setUser updates user and persists to localStorage', () => {
    const store = useUserStore()
    const user = { userId: 1, username: 'admin', role: 'TEACHER' }
    store.setUser(user)

    expect(store.user).toEqual(user)
    expect(JSON.parse(localStorage.getItem('user'))).toEqual(user)
  })

  it('logout clears token and user', () => {
    const store = useUserStore()
    store.setToken('token')
    store.setUser({ username: 'admin' })
    store.logout()

    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('isTeacher returns true only for TEACHER role', () => {
    const store = useUserStore()
    store.setUser({ role: 'TEACHER' })
    expect(store.isTeacher()).toBe(true)

    store.setUser({ role: 'STUDENT' })
    expect(store.isTeacher()).toBe(false)

    store.setUser(null)
    expect(store.isTeacher()).toBe(false)
  })
})
