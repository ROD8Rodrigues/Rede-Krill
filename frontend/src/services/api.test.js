import { beforeEach, describe, expect, it, vi } from 'vitest'
import MockAdapter from 'axios-mock-adapter'
import { api, csrfClient, resetApiState } from './api'

describe('api client', () => {
  let apiMock
  let csrfMock

  beforeEach(() => {
    resetApiState()
    apiMock = new MockAdapter(api)
    csrfMock = new MockAdapter(csrfClient)
  })

  it('adds the CSRF header to mutating requests with the token served by the API', async () => {
    csrfMock.onGet('/auth/csrf').reply(200, { token: 'masked-token' })
    apiMock.onPost('/appointments').reply((config) => {
      expect(config.headers['X-XSRF-TOKEN']).toBe('masked-token')
      return [201, { ok: true }]
    })

    const response = await api.post('/appointments', { orderId: 1 })

    expect(response.status).toBe(201)
    expect(csrfMock.history.get).toHaveLength(1)
  })

  it('requests a new CSRF token for every mutating request', async () => {
    csrfMock.onGet('/auth/csrf').reply(200, { token: 'masked-token' })
    apiMock.onPost('/appointments').reply(201, { ok: true })

    await api.post('/appointments', { orderId: 1 })
    await api.post('/appointments', { orderId: 2 })

    expect(csrfMock.history.get).toHaveLength(2)
  })

  it('disables the automatic XSRF header so the raw cookie value is never sent', () => {
    expect(api.defaults.withXSRFToken).toBe(false)
    expect(csrfClient.defaults.withXSRFToken).toBe(false)
  })

  it('ignores the XSRF-TOKEN cookie because the server rejects the raw value', async () => {
    document.cookie = 'XSRF-TOKEN=raw-cookie-token; path=/'
    csrfMock.onGet('/auth/csrf').reply(200, { token: 'masked-token' })
    apiMock.onPost('/auth/login').reply((config) => {
      expect(config.headers['X-XSRF-TOKEN']).toBe('masked-token')
      return [200, { user: { email: 'admin@redekrill.com' } }]
    })

    const response = await api.post('/auth/login', { email: 'admin@redekrill.com' })

    expect(response.status).toBe(200)
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
  })

  it('refreshes an expired access cookie and retries the original request once', async () => {
    csrfMock.onGet('/auth/csrf').reply(200, { token: 'masked-token' })
    csrfMock.onPost('/auth/refresh').reply(200, { user: { email: 'admin@redekrill.com' } })
    apiMock
      .onGet('/auth/me')
      .replyOnce(401, { message: 'expirado' })
      .onGet('/auth/me')
      .reply(200, { email: 'admin@redekrill.com' })

    const response = await api.get('/auth/me')

    expect(response.status).toBe(200)
    expect(response.data.email).toBe('admin@redekrill.com')
    expect(apiMock.history.get).toHaveLength(2)
    expect(csrfMock.history.post).toHaveLength(1)
  })

  it('notifies the application when the refresh also fails', async () => {
    csrfMock.onGet('/auth/csrf').reply(200, { token: 'masked-token' })
    const handler = vi.fn()
    const { setUnauthorizedHandler } = await import('./api')
    setUnauthorizedHandler(handler)
    csrfMock.onPost('/auth/refresh').reply(401)
    apiMock.onGet('/auth/me').reply(401)

    await expect(api.get('/auth/me')).rejects.toBeTruthy()

    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('does not attempt refresh for login failures', async () => {
    csrfMock.onGet('/auth/csrf').reply(200, { token: 'masked-token' })
    apiMock.onPost('/auth/login').reply(401, { message: 'E-mail ou senha inválidos.' })

    await expect(api.post('/auth/login', {})).rejects.toBeTruthy()

    expect(csrfMock.history.post).toHaveLength(0)
  })
})
