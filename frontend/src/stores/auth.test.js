import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('../services/api', () => ({
  api: {
    get: vi.fn(),
    post: vi.fn()
  },
  ensureCsrf: vi.fn(() => Promise.resolve('csrf-token')),
  setUnauthorizedHandler: vi.fn()
}))

import { api, ensureCsrf, setUnauthorizedHandler } from '../services/api'
import { safeRedirect, useAuthStore } from './auth'

const user = {
  id: 1,
  name: 'Administrador',
  email: 'admin@redekrill.com',
  role: 'ADMIN',
  active: true,
  mustChangePassword: false
}

describe('safeRedirect', () => {
  it('accepts only internal absolute paths', () => {
    expect(safeRedirect('/appointments?page=2')).toBe('/appointments?page=2')
    expect(safeRedirect('/dashboard')).toBe('/dashboard')
  })

  it('rejects external and protocol-relative destinations', () => {
    expect(safeRedirect('https://evil.example')).toBe('/dashboard')
    expect(safeRedirect('//evil.example')).toBe('/dashboard')
    expect(safeRedirect('/\\evil.example')).toBe('/dashboard')
    expect(safeRedirect('javascript:alert(1)')).toBe('/dashboard')
    expect(safeRedirect(null)).toBe('/dashboard')
  })
})

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('logs in using the session returned by the API without localStorage', async () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem')
    api.post.mockResolvedValue({ data: { user } })

    const auth = useAuthStore()
    const result = await auth.login({ email: user.email, password: 'secret' })

    expect(result).toEqual(user)
    expect(auth.user).toEqual(user)
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.isAdmin).toBe(true)
    expect(ensureCsrf).toHaveBeenCalled()
    expect(setItem).not.toHaveBeenCalled()
    setItem.mockRestore()
  })

  it('restores the authenticated user from the HttpOnly session cookie', async () => {
    api.get.mockResolvedValue({ data: user })
    const auth = useAuthStore()

    await auth.initialize()

    expect(api.get).toHaveBeenCalledWith('/auth/me')
    expect(auth.user).toEqual(user)
    expect(auth.initialized).toBe(true)
    expect(setUnauthorizedHandler).toHaveBeenCalled()
  })

  it('keeps the session unauthenticated when the cookie is absent', async () => {
    api.get.mockRejectedValue(new Error('401'))
    const auth = useAuthStore()

    await auth.initialize()

    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('exposes the mandatory password change flag', async () => {
    api.post.mockResolvedValue({
      data: { user: { ...user, mustChangePassword: true } }
    })
    const auth = useAuthStore()

    await auth.login({ email: user.email, password: 'temporary' })

    expect(auth.mustChangePassword).toBe(true)
  })

  it('clears the session on logout even when the server call fails', async () => {
    api.post.mockRejectedValue(new Error('network'))
    const auth = useAuthStore()
    auth.user = user

    await auth.logout()

    expect(auth.user).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
  })
})
