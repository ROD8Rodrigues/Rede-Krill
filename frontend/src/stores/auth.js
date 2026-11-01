import { defineStore } from 'pinia'
import { api, TOKEN_KEY, USER_KEY } from '../services/api'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY))
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readStoredUser(),
    loading: false
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },

  actions: {
    async login(credentials) {
      this.loading = true
      try {
        const { data } = await api.post('/auth/login', credentials)
        this.token = data.token
        this.user = data.user
        localStorage.setItem(TOKEN_KEY, data.token)
        localStorage.setItem(USER_KEY, JSON.stringify(data.user))
        return data.user
      } finally {
        this.loading = false
      }
    },

    async loadCurrentUser() {
      const { data } = await api.get('/auth/me')
      this.user = data
      localStorage.setItem(USER_KEY, JSON.stringify(data))
      return data
    },

    async logout() {
      try {
        if (this.token) {
          await api.post('/auth/logout')
        }
      } catch (error) {
        console.error('Não foi possível registrar o logout na auditoria:', error)
      } finally {
        this.token = ''
        this.user = null
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
      }
    }
  }
})
