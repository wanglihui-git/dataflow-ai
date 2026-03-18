import api from './index'
import type { Pipeline, ExecutionRun } from '@/types'

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
  execute(id: string) {
    return api.post(`/v1/pipelines/${id}/execute`)
  },
  getExecutionRuns(pipelineId: string) {
    return api.get<ExecutionRun[]>(`/v1/execution/runs`, {
      params: { pipelineId }
    })
  }
}
