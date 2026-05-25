import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from '@/types'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  try {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
  } catch {
    /* pinia not ready */
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 200) {
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.clearSession()
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export function unwrap<T>(res: { data: ApiResponse<T> }): T {
  return res.data.data
}

export default http
