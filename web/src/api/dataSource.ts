import http, { unwrap } from './index'
import type { ApiResponse, DataSource, PageResponse } from '@/types'

export async function listDataSources(params?: { page?: number; size?: number }) {
  const res = await http.get<ApiResponse<PageResponse<DataSource> | DataSource[]>>('/v1/data-sources', {
    params
  })
  const data = unwrap(res)
  if (Array.isArray(data)) return { content: data, page: 0, size: data.length, totalElements: data.length, totalPages: 1 }
  return data
}

export async function getDataSource(id: string) {
  const res = await http.get<ApiResponse<DataSource>>(`/v1/data-sources/${id}`)
  return unwrap(res)
}

export async function createDataSource(body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<DataSource>>('/v1/data-sources', body)
  return unwrap(res)
}

export async function updateDataSource(id: string, body: Record<string, unknown>) {
  const res = await http.put<ApiResponse<DataSource>>(`/v1/data-sources/${id}`, body)
  return unwrap(res)
}

export async function deleteDataSource(id: string) {
  await http.delete(`/v1/data-sources/${id}`)
}

export async function testConnection(id: string) {
  const res = await http.post<ApiResponse<Record<string, unknown>>>(`/v1/data-sources/${id}/test`)
  return unwrap(res)
}

export async function previewData(id: string, body?: Record<string, unknown>) {
  const res = await http.post<ApiResponse<Record<string, unknown>>>(`/v1/data-sources/${id}/preview`, body ?? {})
  return unwrap(res)
}

export async function listColumnPermissions(dataSourceId: string) {
  const res = await http.get<ApiResponse<unknown[]>>(`/v1/data-sources/${dataSourceId}/column-permissions`)
  return unwrap(res)
}

export async function createColumnPermission(dataSourceId: string, body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<unknown>>(`/v1/data-sources/${dataSourceId}/column-permissions`, body)
  return unwrap(res)
}

export async function deleteColumnPermission(dataSourceId: string, permId: string) {
  await http.delete(`/v1/data-sources/${dataSourceId}/column-permissions/${permId}`)
}

export async function listRowPermissions(dataSourceId: string) {
  const res = await http.get<ApiResponse<unknown[]>>(`/v1/data-sources/${dataSourceId}/row-permissions`)
  return unwrap(res)
}

export async function createRowPermission(dataSourceId: string, body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<unknown>>(`/v1/data-sources/${dataSourceId}/row-permissions`, body)
  return unwrap(res)
}

export async function deleteRowPermission(dataSourceId: string, permId: string) {
  await http.delete(`/v1/data-sources/${dataSourceId}/row-permissions/${permId}`)
}
