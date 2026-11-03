import { defineStore } from 'pinia'
import { api, ensureCsrf, setUnauthorizedHandler } from '../services/api'

let unauthorizedHandlerRegistered = false

export function safeRedirect(value, fallback = '/dashboard') {
  if (typeof value !== 'string' || value.length === 0) return fallback
  if (!value.startsWith('/')) return fallback
  if (value.startsWith('//') || value.startsWith('/\\')) return fallback
  return value
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    loading: false,
    initialized: false
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.user),
    isAdmin: (state) => state.user?.role === 'ADMIN',
    mustChangePassword: (state) => Boolean(state.user?.mustChangePassword)
  },

  actions: {
    async login(credentials) {
      this.loading = true
      try {
        await ensureCsrf()
        const { data } = await api.post('/auth/login', credentials)
        this.user = data.user
        this.initialized = true
        return data.user
      } finally {
        this.loading = false
      }
    },

    async loadCurrentUser() {
      const { data } = await api.get('/auth/me')
      this.user = data
      return data
    },

    async initialize() {
      if (this.initialized) return this.user
      try {
        await this.loadCurrentUser()
      } catch {
        this.user = null
      } finally {
        this.initialized = true
        this.registerUnauthorizedHandler()
      }
      return this.user
    },

    async changePassword(payload) {
      await ensureCsrf()
      await api.post('/auth/change-password', payload)
      this.user = null
    },

    async logout() {
      try {
        await ensureCsrf()
        await api.post('/auth/logout')
      } catch (error) {
        console.error('Não foi possível registrar o logout no servidor:', error)
      } finally {
        this.user = null
        this.initialized = true
      }
    },

    registerUnauthorizedHandler() {
      if (unauthorizedHandlerRegistered) return
      unauthorizedHandlerRegistered = true
      setUnauthorizedHandler(() => {
        this.user = null
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('auth:expired'))
        }
      })
    }
  }
})
