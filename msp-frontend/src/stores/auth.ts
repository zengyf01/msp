import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api/index'
import type { User, LoginRequest, LoginResponse } from '@/types'

const TOKEN_KEY = 'msp_auth_token'
const USER_KEY = 'msp_auth_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  const isAdmin = computed(() => user.value?.role === 'ADMIN' || user.value?.role === 'NODE_ADMIN')

  async function login(request: LoginRequest) {
    loading.value = true
    try {
      const res = await authAPI.login(request)
      if (res.data.success) {
        const loginData = res.data.data
        token.value = loginData.token
        user.value = {
          userId: loginData.userId,
          username: loginData.username,
          role: loginData.role || loginData.user?.role || 'USER'
        }
        localStorage.setItem(TOKEN_KEY, loginData.token)
        localStorage.setItem(USER_KEY, JSON.stringify(user.value))
        return true
      }
      return false
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    const currentToken = token.value
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    try {
      await authAPI.logout(currentToken)
    } catch {
      // ignore logout errors
    }
  }

  async function fetchCurrentUser() {
    if (!token.value) return null

    try {
      const res = await authAPI.getCurrentUser(token.value)
      if (res.data.success) {
        user.value = res.data.data as unknown as User
        return user.value
      }
    } catch (error) {
      // token invalid or expired
      token.value = null
      user.value = null
    }
    return null
  }

  function initFromStorage() {
    const savedToken = localStorage.getItem(TOKEN_KEY)
    const savedUser = localStorage.getItem(USER_KEY)
    if (savedToken) {
      token.value = savedToken
    }
    if (savedUser) {
      try {
        user.value = JSON.parse(savedUser)
      } catch {
        // ignore
      }
    }
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    isAdmin,
    login,
    logout,
    fetchCurrentUser,
    initFromStorage
  }
})