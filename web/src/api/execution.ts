import http, { unwrap } from './index'
import type { ApiResponse, ExecutionRun, ExecutionStatus, LogEntry, PageResponse } from '@/types'

export async function listRuns(params?: {
  status?: ExecutionStatus
  page?: number
  size?: number
}) {
  const res = await http.get<ApiResponse<PageResponse<ExecutionRun>>>('/v1/execution/runs', { params })
  return unwrap(res)
}

export async function getRun(runId: string) {
  const res = await http.get<ApiResponse<ExecutionRun>>(`/v1/execution/runs/${runId}`)
  return unwrap(res)
}

export async function getRunLogs(runId: string) {
  const res = await http.get<ApiResponse<LogEntry[]>>(`/v1/execution/runs/${runId}/logs`)
  return unwrap(res)
}

export async function cancelRun(runId: string) {
  await http.post(`/v1/execution/runs/${runId}/cancel`)
}

export async function getPipelineStats(pipelineId: string) {
  const res = await http.get<ApiResponse<Record<string, number>>>(
    `/v1/execution/pipelines/${pipelineId}/stats`
  )
  return unwrap(res)
}

export async function listRunsAllStatuses(page = 0, size = 50) {
  const statuses: ExecutionStatus[] = ['RUNNING', 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED']
  const pages = await Promise.all(statuses.map((s) => listRuns({ status: s, page, size })))
  const merged = pages.flatMap((p) => p.content)
  const seen = new Set<string>()
  return merged.filter((r) => {
    if (seen.has(r.id)) return false
    seen.add(r.id)
    return true
  })
}
