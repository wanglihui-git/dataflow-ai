import api from './index'
import type { Pipeline, ExecutionRun, PreviewResult } from '@/types'

export interface RunOptions {
  async?: boolean
  params?: Record<string, unknown>
}

export const pipelineApi = {
  getList() {
    return api.get<Pipeline[]>('/v1/pipelines')
  },
  getById(id: string) {
    return api.get<Pipeline>(`/v1/pipelines/${id}`)
  },
  create(data: Partial<Pipeline>) {
    return api.post<Pipeline>('/v1/pipelines', data)
  },
  update(id: string, data: Partial<Pipeline>) {
    return api.put<Pipeline>(`/v1/pipelines/${id}`, data)
  },
  delete(id: string) {
    return api.delete(`/v1/pipelines/${id}`)
  },
  run(id: string, options?: RunOptions) {
    return api.post<ExecutionRun>(`/v1/pipelines/${id}/run`, options)
  },
  preview(id: string, transformConfig: unknown) {
    return api.post<PreviewResult>(`/v1/pipelines/${id}/preview`, transformConfig)
  },
  getExecutionRuns(pipelineId: string, params?: Record<string, unknown>) {
    return api.get<ExecutionRun[]>(`/v1/pipelines/${pipelineId}/runs`, { params })
  }
}
