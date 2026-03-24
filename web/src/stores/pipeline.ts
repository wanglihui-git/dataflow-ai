import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Pipeline } from '@/types'
import { pipelineApi } from '@/api/pipeline'

export const usePipelineStore = defineStore('pipeline', () => {
  const pipelines = ref<Pipeline[]>([])
  const currentPipeline = ref<Pipeline | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const pipelineList = computed(() => pipelines.value)
  const hasPipelines = computed(() => pipelines.value.length > 0)

  async function fetchPipelines() {
    loading.value = true
    error.value = null
    try {
      const response = await pipelineApi.getList()
      pipelines.value = response.data || []
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取 Pipeline 列表失败'
    } finally {
      loading.value = false
    }
  }

  async function fetchPipelineById(id: string) {
    loading.value = true
    error.value = null
    try {
      const response = await pipelineApi.getById(id)
      currentPipeline.value = response.data || null
      return currentPipeline.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取 Pipeline 详情失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function createPipeline(data: Partial<Pipeline>) {
    loading.value = true
    error.value = null
    try {
      const response = await pipelineApi.create(data)
      const created = response.data
      if (created) {
        pipelines.value.push(created)
      }
      return created
    } catch (e) {
      error.value = e instanceof Error ? e.message : '创建 Pipeline 失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function updatePipeline(id: string, data: Partial<Pipeline>) {
    loading.value = true
    error.value = null
    try {
      const response = await pipelineApi.update(id, data)
      const updated = response.data
      if (updated) {
        const index = pipelines.value.findIndex(p => p.id === id)
        if (index !== -1) {
          pipelines.value[index] = updated
        }
        if (currentPipeline.value?.id === id) {
          currentPipeline.value = updated
        }
      }
      return updated
    } catch (e) {
      error.value = e instanceof Error ? e.message : '更新 Pipeline 失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function deletePipeline(id: string) {
    loading.value = true
    error.value = null
    try {
      await pipelineApi.delete(id)
      pipelines.value = pipelines.value.filter(p => p.id !== id)
      if (currentPipeline.value?.id === id) {
        currentPipeline.value = null
      }
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '删除 Pipeline 失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function runPipeline(id: string, options?: { async?: boolean }) {
    loading.value = true
    error.value = null
    try {
      const response = await pipelineApi.run(id, options)
      return response.data
    } catch (e) {
      error.value = e instanceof Error ? e.message : '执行 Pipeline 失败'
      return null
    } finally {
      loading.value = false
    }
  }

  function clearCurrentPipeline() {
    currentPipeline.value = null
  }

  return {
    pipelines,
    currentPipeline,
    loading,
    error,
    pipelineList,
    hasPipelines,
    fetchPipelines,
    fetchPipelineById,
    createPipeline,
    updatePipeline,
    deletePipeline,
    runPipeline,
    clearCurrentPipeline
  }
})