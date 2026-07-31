import { describe, it, expect, beforeEach } from 'vitest'
import router from '../index'

describe('router guards', () => {
  beforeEach(() => {
    localStorage.clear()
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
})
