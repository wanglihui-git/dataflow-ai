<template>
  <div class="execution-monitor">
    <!-- 筛选工具栏 -->
    <div class="monitor-toolbar">
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 150px">
        <el-option label="全部" value="" />
        <el-option label="等待中" value="PENDING" />
        <el-option label="运行中" value="RUNNING" />
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 执行列表 -->
    <el-table :data="filteredExecutions" v-loading="loading" stripe>
      <el-table-column prop="id" label="执行 ID" width="220" show-overflow-tooltip />
      <el-table-column prop="pipelineId" label="Pipeline" width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <router-link :to="`/pipelines/${row.pipelineId}`" class="pipeline-link">
            {{ row.pipelineId }}
          </router-link>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status" :pulse="row.status === 'RUNNING'" />
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="100">
        <template #default="{ row }">
          {{ calculateDuration(row.startTime, row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="triggeredBy" label="触发者" width="120" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleViewDetail(row)">
            详情
          </el-button>
          <el-button
            v-if="row.status === 'RUNNING' || row.status === 'PENDING'"
            type="danger"
            link
            size="small"
            @click="handleCancel(row)"
          >
            取消
          </el-button>
          <el-button type="success" link size="small" @click="handleDiagnose(row)">
            诊断
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 执行详情对话框 -->
    <el-dialog v-model="detailVisible" title="执行详情" width="800px">
      <div v-if="currentExecution" class="execution-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="执行 ID">{{ currentExecution.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusBadge :status="currentExecution.status" />
          </el-descriptions-item>
          <el-descriptions-item label="Pipeline">{{ currentExecution.pipelineId }}</el-descriptions-item>
          <el-descriptions-item label="触发者">{{ currentExecution.triggeredBy }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ formatDateTime(currentExecution.startTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间">
            {{ formatDateTime(currentExecution.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="耗时" :span="2">
            {{ calculateDuration(currentExecution.startTime, currentExecution.endTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="currentExecution.errorMessage">
          <el-divider content-position="left">错误信息</el-divider>
          <div class="error-message">
            <pre>{{ currentExecution.errorMessage }}</pre>
          </div>
        </template>

        <template v-if="currentExecution.metrics">
          <el-divider content-position="left">执行指标</el-divider>
          <div class="metrics-grid">
            <div v-for="(value, key) in currentExecution.metrics" :key="key" class="metric-item">
              <span class="metric-label">{{ key }}</span>
              <span class="metric-value">{{ value }}</span>
            </div>
          </div>
        </template>

        <el-divider content-position="left">执行日志</el-divider>
        <LogViewer :run-id="currentExecution.id" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import type { ExecutionRun } from '@/types'
import { useExecutionStore } from '@/stores/execution'
import StatusBadge from '@/components/Common/StatusBadge.vue'
import LogViewer from './LogViewer.vue'

const router = useRouter()
const executionStore = useExecutionStore()

const loading = ref(false)
const statusFilter = ref('')
const dateRange = ref<[string, string] | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 详情对话框
const detailVisible = ref(false)
const currentExecution = ref<ExecutionRun | null>(null)

const executions = computed(() => executionStore.executions)

const filteredExecutions = computed(() => {
  let result = executions.value

  if (statusFilter.value) {
    result = result.filter(e => e.status === statusFilter.value)
  }

  if (dateRange.value) {
    const [start, end] = dateRange.value
    result = result.filter(e => {
      const time = new Date(e.createdAt).getTime()
      return time >= new Date(start).getTime() && time <= new Date(end).getTime() + 86400000
    })
  }

  return result
})

const loadExecutions = async () => {
  loading.value = true
  try {
    await executionStore.fetchExecutions()
    total.value = executions.value.length
  } catch (error) {
    ElMessage.error('加载执行记录失败')
  } finally {
    loading.value = false
  }
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const calculateDuration = (startTime?: string, endTime?: string) => {
  if (!startTime || !endTime) return '-'
  const diff = new Date(endTime).getTime() - new Date(startTime).getTime()
  if (diff < 1000) return `${diff}ms`
  if (diff < 60000) return `${(diff / 1000).toFixed(1)}s`
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ${Math.floor((diff % 60000) / 1000)}s`
  return `${Math.floor(diff / 3600000)}h ${Math.floor((diff % 3600000) / 60000)}m`
}

const handleRefresh = () => {
  loadExecutions()
}

const handleViewDetail = async (row: ExecutionRun) => {
  const detail = await executionStore.fetchExecutionDetail(row.id)
  if (detail) {
    currentExecution.value = detail
    detailVisible.value = true
  }
}

const handleCancel = async (row: ExecutionRun) => {
  try {
    await ElMessageBox.confirm('确定要取消该执行吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const success = await executionStore.cancelExecution(row.id)
    if (success) {
      ElMessage.success('取消成功')
      loadExecutions()
    }
  } catch {
    // 用户取消
  }
}

const handleDiagnose = async (row: ExecutionRun) => {
  const result = await executionStore.diagnose(row.id)
  if (result) {
    ElMessage.info('诊断完成，请查看结果')
    // TODO: 显示诊断结果对话框
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadExecutions()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadExecutions()
}

onMounted(() => {
  loadExecutions()
})
</script>

<style scoped>
.execution-monitor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.monitor-toolbar {
  display: flex;
  gap: 12px;
}

.pipeline-link {
  color: var(--color-primary);
  text-decoration: none;
}

.pipeline-link:hover {
  text-decoration: underline;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

.execution-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error-message {
  background: var(--color-danger-light);
  border-radius: var(--radius-md);
  padding: 12px;
}

.error-message pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-danger);
  white-space: pre-wrap;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.metric-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.metric-value {
  font-size: 16px;
  font-weight: 600;
  font-family: var(--font-mono);
}
</style>