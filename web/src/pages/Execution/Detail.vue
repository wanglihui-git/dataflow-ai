<template>
  <div class="page-container" v-loading="loading">
    <PageHeader v-if="run" :title="`运行 ${run.id}`">
      <StatusBadge :status="run.status" />
      <el-button v-if="run.status === 'RUNNING'" type="warning" @click="handleCancel">停止</el-button>
      <el-button @click="handleRetry">重试</el-button>
      <el-button @click="downloadLogs">下载日志</el-button>
      <el-button @click="markHandled">标记已处理</el-button>
    </PageHeader>

    <el-row :gutter="16" v-if="run">
      <el-col :span="8">
        <div class="card-panel summary">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="管道">{{ run.pipelineId }}</el-descriptions-item>
            <el-descriptions-item label="触发者">{{ run.triggeredBy }}</el-descriptions-item>
            <el-descriptions-item label="开始">{{ run.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束">{{ run.endTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="错误">{{ run.errorMessage || '—' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="card-panel">
          <h3>执行步骤</h3>
          <el-tree :data="stepTree" default-expand-all />
        </div>
      </el-col>
      <el-col :span="16">
        <div class="card-panel">
          <div class="log-toolbar">
            <el-input v-model="logFilter" placeholder="过滤日志" clearable size="small" style="width: 200px" />
            <el-button size="small" @click="copyLogs">复制</el-button>
          </div>
          <div ref="logBox" class="log-box">
            <div
              v-for="(entry, i) in filteredLogs"
              :key="i"
              class="log-line"
              :class="{ error: isErrorLine(entry) }"
            >
              <span class="ts">{{ entry.timestamp }}</span>
              <span class="phase">[{{ entry.phase }}]</span>
              {{ entry.message }}
            </div>
          </div>
        </div>
        <div class="card-panel">
          <h3>监控指标</h3>
          <el-row :gutter="12">
            <el-col :span="6" v-for="m in metricCards" :key="m.label">
              <el-statistic :title="m.label" :value="m.value" />
            </el-col>
          </el-row>
          <div ref="metricChart" class="metric-chart" />
          <p class="placeholder-note">CPU/IO 时序需对接 Prometheus（二期）</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useIntervalFn } from '@vueuse/core'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import * as executionApi from '@/api/execution'
import * as pipelineApi from '@/api/pipeline'
import type { ExecutionRun, LogEntry } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'

const route = useRoute()
const router = useRouter()
const runId = route.params.runId as string
const loading = ref(false)
const run = ref<ExecutionRun | null>(null)
const logs = ref<LogEntry[]>([])
const logFilter = ref('')
const logBox = ref<HTMLElement>()
const metricChart = ref<HTMLElement>()
const handled = ref(false)
let chart: echarts.ECharts | null = null

const filteredLogs = computed(() => {
  const q = logFilter.value.toLowerCase()
  if (!q) return logs.value
  return logs.value.filter(
    (e) =>
      (e.message || '').toLowerCase().includes(q) ||
      (e.phase || '').toLowerCase().includes(q)
  )
})

const stepTree = computed(() => {
  const phases = ['INIT', 'SOURCE', 'TRANSFORM', 'SINK']
  const entries = logs.value
  return phases.map((p) => ({
    label: p,
    children: entries
      .filter((e) => e.phase === p)
      .map((e) => ({ label: `${e.timestamp} ${e.message}` }))
  }))
})

const metricCards = computed(() => {
  const m = run.value?.metrics || {}
  return [
    { label: '处理记录', value: String(m.recordsProcessed ?? '—') },
    { label: '耗时(ms)', value: String(m.durationMs ?? '—') },
    { label: '吞吐/秒', value: String(m.recordsPerSecond ?? '—') },
    { label: '内存(MB)', value: String(m.memoryUsedMb ?? '—') }
  ]
})

function isErrorLine(entry: LogEntry) {
  const msg = (entry.message || '').toLowerCase()
  return msg.includes('error') || msg.includes('fail')
}

async function refresh() {
  run.value = await executionApi.getRun(runId)
  logs.value = await executionApi.getRunLogs(runId)
  await nextTick()
  if (logBox.value && run.value?.status === 'RUNNING') {
    logBox.value.scrollTop = logBox.value.scrollHeight
  }
  renderChart()
}

function renderChart() {
  if (!metricChart.value) return
  if (!chart) chart = echarts.init(metricChart.value)
  const m = run.value?.metrics as Record<string, unknown> | undefined
  const tm = (m?.transformMetrics as Record<string, Record<string, number>>) || {}
  const names = Object.keys(tm)
  chart.setOption({
    title: { text: '节点耗时 (ms)', left: 'center', textStyle: { fontSize: 13 } },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: names.map((n) => tm[n]?.durationMs ?? 0) }]
  })
}

const { pause, resume } = useIntervalFn(refresh, 2000)

async function load() {
  loading.value = true
  try {
    await refresh()
    if (run.value?.status === 'RUNNING') resume()
    else pause()
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  await executionApi.cancelRun(runId)
  ElMessage.success('已取消')
  load()
}

async function handleRetry() {
  if (!run.value) return
  const r = await pipelineApi.runPipeline(run.value.pipelineId)
  router.push(`/executions/${r.id}`)
}

function downloadLogs() {
  const blob = new Blob([JSON.stringify(logs.value, null, 2)], { type: 'application/json' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `run-${runId}-logs.json`
  a.click()
}

async function copyLogs() {
  await navigator.clipboard.writeText(logs.value.map((e) => `${e.timestamp} [${e.phase}] ${e.message}`).join('\n'))
  ElMessage.success('已复制')
}

function markHandled() {
  handled.value = true
  ElMessage.success('已标记（仅本地）')
}

onMounted(load)
onUnmounted(() => {
  pause()
  chart?.dispose()
})
</script>

<style scoped>
.log-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.log-box {
  height: 320px;
  overflow: auto;
  background: #0f172a;
  color: #e2e8f0;
  font-family: ui-monospace, monospace;
  font-size: 12px;
  padding: 12px;
  border-radius: 6px;
}
.log-line {
  margin-bottom: 4px;
}
.log-line.error {
  color: #fca5a5;
}
.ts {
  color: #64748b;
  margin-right: 8px;
}
.phase {
  color: #93c5fd;
  margin-right: 6px;
}
.metric-chart {
  height: 220px;
  margin-top: 16px;
}
.placeholder-note {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}
h3 {
  margin: 0 0 12px;
}
</style>
