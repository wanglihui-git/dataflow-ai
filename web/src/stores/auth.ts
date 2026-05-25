import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { LoginResult, UserRole } from '@/types'

const STORAGE_KEY = 'dataflow_auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const userId = ref<string | null>(null)
  const username = ref<string | null>(null)
  const role = ref<UserRole | null>(null)
  const department = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const canWrite = computed(() => role.value === 'ADMIN' || role.value === 'DEVELOPER')

  function persist() {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        token: token.value,
        refreshToken: refreshToken.value,
        userId: userId.value,
        username: username.value,
        role: role.value,
        department: department.value
      })
    )
  }

  function loadFromStorage() {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    try {
      const data = JSON.parse(raw) as LoginResult
      token.value = data.token
      refreshToken.value = data.refreshToken
      userId.value = data.userId
      username.value = data.username
      role.value = data.role
      department.value = data.department ?? null
    } catch {
      clearSession()
    }
  }

  function setSession(data: LoginResult) {
    token.value = data.token
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    username.value = data.username
    role.value = data.role
    department.value = data.department ?? null
    persist()
  }

  function clearSession() {
    token.value = null
    refreshToken.value = null
    userId.value = null
    username.value = null
    role.value = null
    department.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  async function login(user: string, pass: string) {
    const data = await authApi.login(user, pass)
    setSession(data)
    return data
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      /* ignore */
    }
    clearSession()
  }

  loadFromStorage()

  return {
    token,
    refreshToken,
    userId,
    username,
    role,
    department,
    isAuthenticated,
    isAdmin,
    canWrite,
    setSession,
    clearSession,
    login,
    logout
  }
})
