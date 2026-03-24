import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ExecutionRun, ExecutionStats, LogEntry, MetricData } from '@/types'
import { executionApi } from '@/api/execution'

export const useExecutionStore = defineStore('execution', () => {
  const executions = ref<ExecutionRun[]>([])
  const currentExecution = ref<ExecutionRun | null>(null)
  const executionStats = ref<ExecutionStats | null>(null)
  const executionLogs = ref<LogEntry[]>([])
  const executionMetrics = ref<MetricData | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 运行中的执行
  const runningExecutions = computed(() =>
    executions.value.filter(e => e.status === 'RUNNING' || e.status === 'PENDING')
  )

  // 最近的执行
  const recentExecutions = computed(() =>
    [...executions.value]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 10)
  )

  // 获取执行列表
  async function fetchExecutions(pipelineId?: string) {
    loading.value = true
    error.value = null
    try {
      // TODO: 需要一个获取所有执行的 API
      executions.value = []
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取执行记录失败'
    } finally {
      loading.value = false
    }
  }

  // 获取执行详情
  async function fetchExecutionDetail(runId: string) {
    loading.value = true
    error.value = null
    try {
      const response = await executionApi.getDetail(runId)
      currentExecution.value = response.data || null
      return currentExecution.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取执行详情失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 取消执行
  async function cancelExecution(runId: string) {
    loading.value = true
    error.value = null
    try {
      await executionApi.cancel(runId)
      // 更新当前执行状态
      if (currentExecution.value?.id === runId) {
        currentExecution.value.status = 'CANCELLED'
      }
      // 更新列表中的状态
      const index = executions.value.findIndex(e => e.id === runId)
      if (index !== -1) {
        executions.value[index].status = 'CANCELLED'
      }
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '取消执行失败'
      return false
    } finally {
      loading.value = false
    }
  }

  // 获取执行统计
  async function fetchExecutionStats(pipelineId: string, timeframe?: string) {
    loading.value = true
    error.value = null
    try {
      const response = await executionApi.getStats(pipelineId, timeframe)
      executionStats.value = response.data || null
      return executionStats.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取执行统计失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取执行日志
  async function fetchExecutionLogs(runId: string) {
    loading.value = true
    error.value = null
    try {
      const response = await executionApi.getLogs(runId)
      executionLogs.value = response.data || []
      return executionLogs.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取执行日志失败'
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取执行指标
  async function fetchExecutionMetrics(runId: string) {
    loading.value = true
    error.value = null
    try {
      const response = await executionApi.getMetrics(runId)
      executionMetrics.value = response.data || null
      return executionMetrics.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取执行指标失败'
      return null
    } finally {
      loading.value = false
    }
  }

  // 清理当前执行
  function clearCurrentExecution() {
    currentExecution.value = null
    executionStats.value = null
    executionLogs.value = []
    executionMetrics.value = null
  }

  return {
    executions,
    currentExecution,
    executionStats,
    executionLogs,
    executionMetrics,
    loading,
    error,
    runningExecutions,
    recentExecutions,
    fetchExecutions,
    fetchExecutionDetail,
    cancelExecution,
    fetchExecutionStats,
    fetchExecutionLogs,
    fetchExecutionMetrics,
    clearCurrentExecution
  }
})