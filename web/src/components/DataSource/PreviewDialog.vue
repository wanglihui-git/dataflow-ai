<template>
  <el-dialog
    v-model="visible"
    title="数据预览"
    width="900px"
    :close-on-click-modal="false"
  >
    <div class="preview-container">
      <div class="preview-toolbar">
        <el-select v-model="viewMode" size="small" style="width: 120px">
          <el-option label="表格视图" value="table" />
          <el-option label="JSON 视图" value="json" />
        </el-select>
        <span class="preview-count">共 {{ total }} 条记录</span>
        <el-button size="small" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div v-loading="loading" class="preview-content">
        <!-- 表格视图 -->
        <el-table
          v-if="viewMode === 'table' && columns.length"
          :data="rows"
          stripe
          border
          height="400px"
          style="width: 100%"
        >
          <el-table-column
            v-for="col in columns"
            :key="col"
            :prop="col"
            :label="col"
            min-width="150"
            show-overflow-tooltip
          />
        </el-table>

        <!-- JSON 视图 -->
        <pre v-else-if="viewMode === 'json'" class="json-view">{{ jsonContent }}</pre>

        <!-- 空状态 -->
        <EmptyState
          v-else
          type="data"
          title="暂无数据"
          description="该数据源暂无数据，请检查数据源配置"
        />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import EmptyState from '@/components/Common/EmptyState.vue'
import type { PreviewData } from '@/types'
import { dataSourceApi } from '@/api/dataSource'

const props = defineProps<{
  modelValue: boolean
  dataSourceId: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const viewMode = ref<'table' | 'json'>('table')
const previewData = ref<PreviewData | null>(null)

const columns = computed(() => previewData.value?.columns || [])
const rows = computed(() => previewData.value?.rows || [])
const total = computed(() => previewData.value?.total || 0)

const jsonContent = computed(() => {
  if (!previewData.value?.rows.length) return ''
  return JSON.stringify(previewData.value.rows.slice(0, 10), null, 2)
})

const fetchPreview = async () => {
  if (!props.dataSourceId) return

  loading.value = true
  try {
    const result = await dataSourceApi.preview(props.dataSourceId, { limit: 100 })
    previewData.value = result
  } catch (error) {
    console.error('预览数据失败:', error)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchPreview()
}

watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal && props.dataSourceId) {
      fetchPreview()
    }
  }
)
</script>

<style scoped>
.preview-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.preview-count {
  color: var(--text-secondary);
  font-size: 14px;
}

.preview-content {
  min-height: 300px;
}

.json-view {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  max-height: 400px;
  overflow: auto;
  margin: 0;
}
</style>