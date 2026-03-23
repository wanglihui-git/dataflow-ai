<template>
  <div class="execution-detail-page">
    <el-button @click="handleBack">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <el-card v-if="execution" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>执行详情</span>
          <StatusBadge :status="execution.status" :pulse="execution.status === 'RUNNING'" />
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="执行 ID">{{ execution.id }}</el-descriptions-item>
        <el-descriptions-item label="Pipeline">
          <router-link :to="`/pipelines/${execution.pipelineId}`">
            {{ execution.pipelineId }}
          </router-link>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ formatDateTime(execution.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ formatDateTime(execution.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="触发者">{{ execution.triggeredBy }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(execution.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <template v-if="execution.errorMessage">
        <el-divider content-position="left">错误信息</el-divider>
        <div class="error-block">
          <pre>{{ execution.errorMessage }}</pre>
        </div>
      </template>
    </el-card>

    <el-card v-if="execution?.metrics" class="metrics-card">
      <template #header>
        <span>执行指标</span>
      </template>
      <div class="metrics-grid">
        <div v-for="(value, key) in execution.metrics" :key="key" class="metric-item">
          <span class="metric-label">{{ key }}</span>
          <span class="metric-value">{{ value }}</span>
        </div>
      </div>
    </el-card>

    <el-card class="logs-card">
      <template #header>
        <span>执行日志</span>
      </template>
      <LogViewer :run-id="runId" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { ExecutionRun } from '@/types'
import { useExecutionStore } from '@/stores/execution'
import StatusBadge from '@/components/Common/StatusBadge.vue'
import LogViewer from '@/components/Execution/LogViewer.vue'

const route = useRoute()
const router = useRouter()
const executionStore = useExecutionStore()

const runId = computed(() => route.params.runId as string)
const execution = computed(() => executionStore.currentExecution)

const loadExecution = async () => {
  await executionStore.fetchExecutionDetail(runId.value)
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  loadExecution()
})
</script>

<style scoped>
.execution-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-card,
.metrics-card,
.logs-card {
  margin-bottom: 0;
}

.error-block {
  background: var(--color-danger-light);
  border-radius: var(--radius-md);
  padding: 12px;
}

.error-block pre {
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