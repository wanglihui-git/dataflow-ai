import api from './index'
import type { ExecutionRun, ExecutionStats, LogEntry, MetricData } from '@/types'

export const executionApi = {
  getDetail(runId: string) {
    return api.get<ExecutionRun>(`/v1/execution/runs/${runId}`)
  },
  cancel(runId: string) {
    return api.post(`/v1/execution/runs/${runId}/cancel`)
  },
  getStats(pipelineId: string, timeframe?: string) {
    return api.get<ExecutionStats>(`/v1/execution/pipelines/${pipelineId}/stats`, {
      params: { timeframe }
    })
  },
  getLogs(runId: string) {
    return api.get<LogEntry[]>(`/v1/execution/runs/${runId}/logs`)
  },
  getMetrics(runId: string) {
    return api.get<MetricData>(`/v1/execution/runs/${runId}/metrics`)
  }
}