import api from './index'
import type { DataSource, ConnectionTestResult, PreviewData, PreviewConfig } from '@/types'

export const dataSourceApi = {
  getList() {
    return api.get<DataSource[]>('/v1/data-sources')
  },
  getById(id: string) {
    return api.get<DataSource>(`/v1/data-sources/${id}`)
  },
  create(data: Partial<DataSource>) {
    return api.post<DataSource>('/v1/data-sources', data)
  },
  update(id: string, data: Partial<DataSource>) {
    return api.put<DataSource>(`/v1/data-sources/${id}`, data)
  },
  delete(id: string) {
    return api.delete(`/v1/data-sources/${id}`)
  },
  testConnection(id: string) {
    return api.post<ConnectionTestResult>(`/v1/data-sources/${id}/test`)
  },
  preview(id: string, config?: PreviewConfig) {
    return api.post<PreviewData>(`/v1/data-sources/${id}/preview`, config)
  }
}
