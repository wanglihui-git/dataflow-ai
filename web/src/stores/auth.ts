import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest } from '@/types'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<User | null>(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  const isAuthenticated = computed(() => !!token.value)

  async function login(credentials: LoginRequest) {
    const response = await authApi.login(credentials)
    // response.data 是 ApiResponse，嵌套的 data 才是 LoginResponse
    const { token: newToken, ...userData } = response.data.data

    token.value = newToken
    user.value = userData as User

    localStorage.setItem('token', newToken)
    localStorage.setItem('user', JSON.stringify(userData))

    return response.data
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    logout
  }
})
