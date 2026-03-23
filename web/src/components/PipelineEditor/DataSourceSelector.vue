<template>
  <el-dialog
    v-model="visible"
    title="选择数据源"
    width="700px"
    :close-on-click-modal="false"
  >
    <div class="data-source-selector">
      <!-- 搜索 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索数据源名称"
          prefix-icon="Search"
          clearable
        />
      </div>

      <!-- 数据源列表 -->
      <div v-loading="loading" class="data-source-list">
        <div
          v-for="ds in filteredDataSources"
          :key="ds.id"
          class="data-source-item"
          :class="{ selected: selectedId === ds.id }"
          @click="handleSelect(ds)"
        >
          <div class="ds-info">
            <div class="ds-name">{{ ds.name }}</div>
            <div class="ds-meta">
              <el-tag size="small">{{ typeLabels[ds.type] }}</el-tag>
              <span class="ds-time">创建于 {{ formatDate(ds.createdAt) }}</span>
            </div>
          </div>
          <div class="ds-actions">
            <el-button size="small" @click.stop="handlePreview(ds)">
              预览
            </el-button>
            <el-button size="small" type="success" @click.stop="handleTest(ds.id)">
              测试
            </el-button>
          </div>
        </div>

        <EmptyState
          v-if="!filteredDataSources.length"
          type="data"
          title="暂无数据源"
          description="请先创建数据源"
        />
      </div>
    </div>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="数据预览"
      width="600px"
      append-to-body
    >
      <pre v-if="previewData">{{ JSON.stringify(previewData, null, 2) }}</pre>
      <EmptyState v-else type="data" title="暂无数据" />
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { DataSource } from '@/types'
import { dataSourceApi } from '@/api/dataSource'
import EmptyState from '@/components/Common/EmptyState.vue'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', dataSource: DataSource, preview: Record<string, unknown>): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const typeLabels: Record<string, string> = {
  MYSQL: 'MySQL',
  POSTGRES: 'PostgreSQL',
  API: 'API',
  KAFKA: 'Kafka',
  CSV: 'CSV'
}

const loading = ref(false)
const dataSources = ref<DataSource[]>([])
const searchKeyword = ref('')
const selectedId = ref('')

// 预览
const previewVisible = ref(false)
const previewData = ref<Record<string, unknown> | null>(null)

const filteredDataSources = computed(() => {
  if (!searchKeyword.value) return dataSources.value
  return dataSources.value.filter(ds =>
    ds.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const loadDataSources = async () => {
  loading.value = true
  try {
    const res = await dataSourceApi.getList()
    dataSources.value = res.data.data || []
  } catch (error) {
    ElMessage.error('加载数据源失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const handleSelect = async (ds: DataSource) => {
  selectedId.value = ds.id

  // 加载预览数据
  try {
    const result = await dataSourceApi.preview(ds.id, { limit: 1 })
    const data = result.data.data
    if (data?.rows?.length) {
      emit('select', ds, data.rows[0])
      visible.value = false
    } else {
      // 即使没有数据也允许选择
      emit('select', ds, {})
    }
  } catch (error) {
    // 即使预览失败也允许选择
    emit('select', ds, {})
  }
}

const handlePreview = async (ds: DataSource) => {
  try {
    const result = await dataSourceApi.preview(ds.id, { limit: 10 })
    previewData.value = result.data.data?.rows?.[0] || null
    previewVisible.value = true
  } catch (error) {
    ElMessage.error('预览失败')
  }
}

const handleTest = async (id: string) => {
  try {
    await dataSourceApi.testConnection(id)
    ElMessage.success('连接成功')
  } catch (error) {
    ElMessage.error('连接失败')
  }
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped>
.data-source-selector {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  padding: 0;
}

.data-source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.data-source-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.data-source-item:hover {
  background: var(--bg-secondary);
}

.data-source-item.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.ds-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ds-name {
  font-weight: 500;
  color: var(--text-primary);
}

.ds-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ds-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.ds-actions {
  display: flex;
  gap: 8px;
}
</style>