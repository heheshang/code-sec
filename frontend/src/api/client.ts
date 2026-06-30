import axios from 'axios'

/**
 * Single shared Axios instance. When MSW is initialized, the worker intercepts
 * all /api/v1/* requests and returns mock data without hitting the network.
 *
 * Interceptors:
 *  - request: attach a stable mock user id (no real auth in the prototype)
 *  - response: normalize error shape so the UI can show a single toast
 */
export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  // Placeholder for the real OAuth2 access token; left as a constant
  // because the prototype has no auth flow.
  config.headers.set('X-Current-User', 'user-current-auditor')
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const message =
      error.response?.data?.message ??
      error.message ??
      'Request failed'
    return Promise.reject(new Error(message))
  },
)
