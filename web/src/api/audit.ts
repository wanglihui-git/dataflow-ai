import http, { unwrap } from './index'
import type { ApiResponse, AuditLog } from '@/types'

export async function listAuditLogs(params?: Record<string, unknown>) {
  const res = await http.get<ApiResponse<AuditLog[]>>('/v1/audit-logs', { params })
  return unwrap(res)
}
