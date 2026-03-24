import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { DataSource, ConnectionTestResult, PreviewData } from '@/types'
import { dataSourceApi } from '@/api/dataSource'

export const useDataSourceStore = defineStore('dataSource', () => {
  const dataSources = ref<DataSource[]>([])
  const currentDataSource = ref<DataSource | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const dataSourceList = computed(() => dataSources.value)
  const hasDataSources = computed(() => dataSources.value.length > 0)

  async function fetchDataSources() {
    loading.value = true
    error.value = null
    try {
      const response = await dataSourceApi.getList()
      dataSources.value = response.data || []
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取数据源列表失败'
    } finally {
      loading.value = false
    }
  }

  async function fetchDataSourceById(id: string) {
    loading.value = true
    error.value = null
    try {
      const response = await dataSourceApi.getById(id)
      currentDataSource.value = response.data || null
      return currentDataSource.value
    } catch (e) {
      error.value = e instanceof Error ? e.message : '获取数据源详情失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function createDataSource(data: Partial<DataSource>) {
    loading.value = true
    error.value = null
    try {
      const response = await dataSourceApi.create(data)
      const created = response.data
      if (created) {
        dataSources.value.push(created)
      }
      return created
    } catch (e) {
      error.value = e instanceof Error ? e.message : '创建数据源失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function updateDataSource(id: string, data: Partial<DataSource>) {
    loading.value = true
    error.value = null
    try {
      const response = await dataSourceApi.update(id, data)
      const updated = response.data
      if (updated) {
        const index = dataSources.value.findIndex(ds => ds.id === id)
        if (index !== -1) {
          dataSources.value[index] = updated
        }
        if (currentDataSource.value?.id === id) {
          currentDataSource.value = updated
        }
      }
      return updated
    } catch (e) {
      error.value = e instanceof Error ? e.message : '更新数据源失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function deleteDataSource(id: string) {
    loading.value = true
    error.value = null
    try {
      await dataSourceApi.delete(id)
      dataSources.value = dataSources.value.filter(ds => ds.id !== id)
      if (currentDataSource.value?.id === id) {
        currentDataSource.value = null
      }
      return true
    } catch (e) {
      error.value = e instanceof Error ? e.message : '删除数据源失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function testConnection(id: string): Promise<ConnectionTestResult | null> {
    loading.value = true
    error.value = null
    try {
      const response = await dataSourceApi.testConnection(id)
      return response.data
    } catch (e) {
      error.value = e instanceof Error ? e.message : '测试连接失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function previewData(id: string, config?: { limit?: number; offset?: number; query?: string }): Promise<PreviewData | null> {
    loading.value = true
    error.value = null
    try {
      const response = await dataSourceApi.preview(id, config)
      return response.data
    } catch (e) {
      error.value = e instanceof Error ? e.message : '预览数据失败'
      return null
    } finally {
      loading.value = false
    }
  }

  function clearCurrentDataSource() {
    currentDataSource.value = null
  }

  return {
    dataSources,
    currentDataSource,
    loading,
    error,
    dataSourceList,
    hasDataSources,
    fetchDataSources,
    fetchDataSourceById,
    createDataSource,
    updateDataSource,
    deleteDataSource,
    testConnection,
    previewData,
    clearCurrentDataSource
  }
})