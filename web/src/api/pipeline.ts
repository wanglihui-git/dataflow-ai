import http, { unwrap } from './index'
import type { ApiResponse, ExecutionRun, PageResponse, Pipeline } from '@/types'

export async function listPipelines(params?: { name?: string; page?: number; size?: number }) {
  const res = await http.get<ApiResponse<PageResponse<Pipeline>>>('/v1/pipelines', { params })
  return unwrap(res)
}

export async function getPipeline(id: string) {
  const res = await http.get<ApiResponse<Pipeline>>(`/v1/pipelines/${id}`)
  return unwrap(res)
}

export async function createPipeline(body: Record<string, unknown>) {
  const res = await http.post<ApiResponse<Pipeline>>('/v1/pipelines', body)
  return unwrap(res)
}

export async function updatePipeline(id: string, body: Record<string, unknown>) {
  const res = await http.put<ApiResponse<Pipeline>>(`/v1/pipelines/${id}`, body)
  return unwrap(res)
}

export async function deletePipeline(id: string) {
  await http.delete(`/v1/pipelines/${id}`)
}

export async function runPipeline(id: string) {
  const res = await http.post<ApiResponse<ExecutionRun>>(`/v1/pipelines/${id}/run`)
  return unwrap(res)
}

export async function listPipelineRuns(id: string) {
  const res = await http.get<ApiResponse<ExecutionRun[]>>(`/v1/pipelines/${id}/runs`)
  return unwrap(res)
}

export async function previewPipeline(id: string) {
  const res = await http.get<ApiResponse<Record<string, unknown>>>(`/v1/pipelines/${id}/preview`)
  return unwrap(res)
}
