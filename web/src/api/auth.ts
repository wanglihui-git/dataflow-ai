import api from './index'
import type { LoginRequest, LoginResponse } from '@/types'

export const authApi = {
  login(data: LoginRequest) {
    return api.post<LoginResponse>('/v1/auth/login', data)
  },
  logout() {
    return api.post('/v1/auth/logout')
  }
}
