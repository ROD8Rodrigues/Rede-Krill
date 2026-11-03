import axios from 'axios'

const CSRF_HEADER = 'X-XSRF-TOKEN'
const AUTH_ENDPOINTS_WITHOUT_REFRESH = ['/auth/login', '/auth/refresh', '/auth/csrf']
const MUTATING_METHODS = ['post', 'put', 'patch', 'delete']

let refreshPromise = null
let unauthorizedHandler = null

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  withXSRFToken: false,
  headers: {
    'Content-Type': 'application/json'
  }
})

const rawClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  withXSRFToken: false,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const csrfClient = rawClient

export function resetApiState() {
  refreshPromise = null
  unauthorizedHandler = null
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export async function ensureCsrf() {
  // O cookie XSRF-TOKEN carrega o token bruto; o servidor só aceita o token mascarado
  // que sai no corpo de GET /auth/csrf (proteção BREACH do Spring Security).
  // O login e o logout geram um token bruto novo, por isso buscamos um mascarado
  // novo a cada requisição de escrita em vez de guardar o anterior.
  const { data } = await rawClient.get('/auth/csrf')
  return data.token
}

function isAuthEndpointWithoutRefresh(url = '') {
  return AUTH_ENDPOINTS_WITHOUT_REFRESH.some((endpoint) => url.includes(endpoint))
}

api.interceptors.request.use(async (config) => {
  if (MUTATING_METHODS.includes((config.method || 'get').toLowerCase())) {
    const token = await ensureCsrf()
    config.headers[CSRF_HEADER] = token
  }
  return config
})

async function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const token = await ensureCsrf()
      const { data } = await rawClient.post('/auth/refresh', null, {
        headers: { [CSRF_HEADER]: token }
      })
      return data
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status

    if (
      status === 401 &&
      originalRequest &&
      !originalRequest._retriedAfterRefresh &&
      !isAuthEndpointWithoutRefresh(originalRequest.url)
    ) {
      originalRequest._retriedAfterRefresh = true
      try {
        await refreshSession()
        return api(originalRequest)
      } catch {
        if (typeof unauthorizedHandler === 'function') {
          unauthorizedHandler()
        }
      }
    }

    return Promise.reject(error)
  }
)

export { CSRF_HEADER }
