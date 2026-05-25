<template>
  <div class="page-container">
    <PageHeader title="运行任务" subtitle="管理与监控 Pipeline 执行">
      <el-button type="primary" @click="showTrigger = true">手动触发</el-button>
    </PageHeader>

    <el-tabs v-model="statusTab" @tab-change="load">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="运行中" name="RUNNING" />
      <el-tab-pane label="成功" name="SUCCESS" />
      <el-tab-pane label="失败" name="FAILED" />
      <el-tab-pane label="已取消" name="CANCELLED" />
    </el-tabs>

    <div class="card-panel" v-loading="loading">
      <el-table :data="rows">
        <el-table-column prop="id" label="运行 ID" min-width="140" show-overflow-tooltip />
        <el-table-column label="管道" min-width="140">
          <template #default="{ row }">{{ pipeNames[row.pipelineId] || row.pipelineId }}</template>
        </el-table-column>
        <el-table-column prop="triggeredBy" label="触发者" width="100" />
        <el-table-column prop="startTime" label="开始" width="170" />
        <el-table-column prop="endTime" label="结束" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ formatDuration(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/executions/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'RUNNING'" link type="warning" @click="handleCancel(row.id)">
              停止
            </el-button>
            <el-button link @click="handleRetry(row.pipelineId)">重试</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        class="pager"
        @change="load"
      />
    </div>

    <el-dialog v-model="showTrigger" title="手动触发运行" width="400px">
      <el-select v-model="triggerPipelineId" filterable placeholder="选择 Pipeline" style="width: 100%">
        <el-option v-for="p in pipelines" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <template #footer>
        <el-button @click="showTrigger = false">取消</el-button>
        <el-button type="primary" @click="submitTrigger">运行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as executionApi from '@/api/execution'
import * as pipelineApi from '@/api/pipeline'
import type { ExecutionRun, ExecutionStatus, Pipeline } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'

const router = useRouter()
const loading = ref(false)
const rows = ref<ExecutionRun[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const statusTab = ref('ALL')
const pipeNames = ref<Record<string, string>>({})
const pipelines = ref<Pipeline[]>([])
const showTrigger = ref(false)
const triggerPipelineId = ref('')

function formatDuration(row: ExecutionRun) {
  if (!row.startTime || !row.endTime) return '—'
  const ms = new Date(row.endTime).getTime() - new Date(row.startTime).getTime()
  return `${Math.round(ms / 1000)}s`
}

async function load() {
  loading.value = true
  try {
    if (statusTab.value === 'ALL') {
      const all = await executionApi.listRunsAllStatuses(page.value - 1, size.value)
      rows.value = all.slice(0, size.value)
      total.value = all.length
    } else {
      const res = await executionApi.listRuns({
        status: statusTab.value as ExecutionStatus,
        page: page.value - 1,
        size: size.value
      })
      rows.value = res.content
      total.value = res.totalElements
    }
  } finally {
    loading.value = false
  }
}

async function handleCancel(runId: string) {
  await executionApi.cancelRun(runId)
  ElMessage.success('已请求取消')
  load()
}

async function handleRetry(pipelineId: string) {
  const run = await pipelineApi.runPipeline(pipelineId)
  router.push(`/executions/${run.id}`)
}

async function submitTrigger() {
  if (!triggerPipelineId.value) return
  const run = await pipelineApi.runPipeline(triggerPipelineId.value)
  showTrigger.value = false
  router.push(`/executions/${run.id}`)
}

onMounted(async () => {
  const pipes = await pipelineApi.listPipelines({ page: 0, size: 200 })
  pipelines.value = pipes.content
  pipeNames.value = Object.fromEntries(pipes.content.map((p) => [p.id, p.name]))
  load()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
