import axios from 'axios'

/**
 * Single shared Axios instance for the CodeSec backend API.
 * All requests go to the real backend via Vite proxy.
 * Mock data (MSW) has been fully removed.
 */
export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

function getToken(): string | null {
  return localStorage.getItem('token')
}

export function setToken(token: string): void {
  localStorage.setItem('token', token)
}

export function clearToken(): void {
  localStorage.removeItem('token')
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearToken()
      window.location.href = '/login'
    }
    const message =
      error.response?.data?.message ??
      error.message ??
      'Request failed'
    return Promise.reject(new Error(message))
  },
)
