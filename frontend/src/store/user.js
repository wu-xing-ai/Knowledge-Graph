import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  function setToken(t) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUser(u) {
    user.value = u
    localStorage.setItem('user', JSON.stringify(u))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    if (res.code === 200) {
      setUser(res.data)
    }
  }

  function isTeacher() {
    // 管理员继承教师功能
    return user.value?.role === 'TEACHER' || user.value?.role === 'ADMIN'
  }

  function isAdmin() {
    return user.value?.role === 'ADMIN'
  }

  return { token, user, setToken, setUser, logout, fetchUserInfo, isTeacher, isAdmin }
})

