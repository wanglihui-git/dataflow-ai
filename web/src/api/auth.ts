import http, { unwrap } from './index'
import type { ApiResponse, LoginResult } from '@/types'

export async function login(username: string, password: string) {
  const res = await http.post<ApiResponse<LoginResult>>('/v1/auth/login', { username, password })
  return unwrap(res)
}

export async function refresh(refreshToken: string) {
  const res = await http.post<ApiResponse<LoginResult>>('/v1/auth/refresh', { refreshToken })
  return unwrap(res)
}

export async function logout() {
  await http.post('/v1/auth/logout')
}
