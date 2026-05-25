import http, { unwrap } from './index'
import type { ApiResponse, UserVO } from '@/types'

export async function listUsers() {
  const res = await http.get<ApiResponse<UserVO[]>>('/v1/users')
  return unwrap(res)
}

export async function getUser(id: string) {
  const res = await http.get<ApiResponse<UserVO>>(`/v1/users/${id}`)
  return unwrap(res)
}

export async function createUser(body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<UserVO>>('/v1/users', body)
  return unwrap(res)
}

export async function updateUser(id: string, body: Record<string, unknown>) {
  const res = await http.put<ApiResponse<UserVO>>(`/v1/users/${id}`, body)
  return unwrap(res)
}

export async function deleteUser(id: string) {
  await http.delete(`/v1/users/${id}`)
}

export async function changePassword(oldPassword: string, newPassword: string) {
  await http.put('/v1/users/me/password', { oldPassword, newPassword })
}
