<template>
  <div class="page-container">
    <PageHeader title="Dashboard" subtitle="平台关键指标与运行概览">
      <el-select v-model="range" style="width: 120px" @change="load">
        <el-option label="24 小时" value="24h" />
        <el-option label="7 天" value="7d" />
        <el-option label="30 天" value="30d" />
      </el-select>
      <el-button @click="router.push('/pipelines/create')">新建 Pipeline</el-button>
      <el-button @click="router.push('/data-sources')">新建数据源</el-button>
      <el-button type="primary" @click="router.push('/executions')">查看运行</el-button>
    </PageHeader>

    <el-row :gutter="16" class="kpi-row" v-loading="loading">
      <el-col :span="6" v-for="k in kpis" :key="k.label">
        <div class="card-panel kpi-card">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value">{{ k.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="16">
        <div class="card-panel">
          <h3>任务成功 / 失败趋势</h3>
          <div ref="chartRef" class="chart-box" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="card-panel">
          <h3>当前运行中</h3>
          <div class="running-count">{{ runningCount }}</div>
          <p class="muted">队列中的 RUNNING 任务数</p>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="card-panel">
          <h3>最近运行</h3>
          <el-table :data="recentRuns" @row-click="(r: ExecutionRun) => goRun(r.id)">
            <el-table-column prop="id" label="运行 ID" min-width="120" show-overflow-tooltip />
            <el-table-column label="管道" min-width="120">
              <template #default="{ row }">{{ pipelineNames[row.pipelineId] || row.pipelineId }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><StatusBadge :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="triggeredBy" label="触发者" width="100" />
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="card-panel">
          <h3>最近编辑的 Pipeline</h3>
          <div v-for="p in recentPipelines" :key="p.id" class="pipe-mini" @click="router.push(`/pipelines/${p.id}`)">
            <span>{{ p.name }}</span>
            <StatusBadge :status="p.status" />
          </div>
          <EmptyState v-if="!recentPipelines.length" title="暂无管道" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import * as pipelineApi from '@/api/pipeline'
import * as executionApi from '@/api/execution'
import * as userApi from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { ExecutionRun, Pipeline } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const range = ref('24h')
const runningCount = ref(0)
const recentRuns = ref<ExecutionRun[]>([])
const recentPipelines = ref<Pipeline[]>([])
const pipelineNames = ref<Record<string, string>>({})
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const kpis = computed(() => [
  { label: '已注册用户数', value: userCount.value },
  { label: '活跃管道数', value: pipelineCount.value },
  { label: '近 24h 成功率', value: successRate.value },
  { label: '队列长度', value: runningCount.value }
])

const userCount = ref('—')
const pipelineCount = ref('—')
const successRate = ref('—')

function rangeMs() {
  const map: Record<string, number> = { '24h': 86400000, '7d': 7 * 86400000, '30d': 30 * 86400000 }
  return map[range.value] ?? 86400000
}

function inRange(iso?: string) {
  if (!iso) return false
  return Date.now() - new Date(iso).getTime() <= rangeMs()
}

async function load() {
  loading.value = true
  try {
    if (auth.isAdmin) {
      const users = await userApi.listUsers()
      userCount.value = String(users.length)
    } else {
      userCount.value = '—'
    }
    const pipes = await pipelineApi.listPipelines({ page: 0, size: 100 })
    pipelineCount.value = String(
      pipes.content.filter((p) => (p.status || 'active') === 'active').length
    )
    recentPipelines.value = [...pipes.content]
      .sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
      .slice(0, 5)
    pipelineNames.value = Object.fromEntries(pipes.content.map((p) => [p.id, p.name]))

    const running = await executionApi.listRuns({ status: 'RUNNING', page: 0, size: 1 })
    runningCount.value = running.totalElements

    const all = await executionApi.listRunsAllStatuses(0, 100)
    const filtered = all.filter((r) => inRange(r.startTime || r.createdAt))
    const ok = filtered.filter((r) => r.status === 'SUCCESS').length
    const fail = filtered.filter((r) => r.status === 'FAILED').length
    const denom = ok + fail
    successRate.value = denom ? `${Math.round((ok / denom) * 100)}%` : '—'

    recentRuns.value = [...filtered]
      .sort((a, b) => (b.startTime || '').localeCompare(a.startTime || ''))
      .slice(0, 20)

    renderChart(filtered)
  } finally {
    loading.value = false
  }
}

function renderChart(runs: ExecutionRun[]) {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const buckets = new Map<string, { success: number; failed: number }>()
  for (const r of runs) {
    const d = (r.startTime || r.createdAt || '').slice(0, 10)
    if (!d) continue
    const b = buckets.get(d) || { success: 0, failed: 0 }
    if (r.status === 'SUCCESS') b.success++
    if (r.status === 'FAILED') b.failed++
    buckets.set(d, b)
  }
  const dates = [...buckets.keys()].sort()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功', '失败'] },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [
      { name: '成功', type: 'line', data: dates.map((d) => buckets.get(d)!.success), smooth: true },
      { name: '失败', type: 'line', data: dates.map((d) => buckets.get(d)!.failed), smooth: true }
    ]
  })
}

function goRun(id: string) {
  router.push(`/executions/${id}`)
}

onMounted(() => {
  load()
  window.addEventListener('resize', () => chart?.resize())
})
onUnmounted(() => chart?.dispose())
</script>

<style scoped>
.kpi-card .kpi-label {
  font-size: 13px;
  color: #64748b;
}
.kpi-value {
  font-size: 28px;
  font-weight: 600;
  margin-top: 8px;
}
.chart-box {
  height: 280px;
}
.running-count {
  font-size: 48px;
  font-weight: 700;
  color: var(--color-running);
}
.muted {
  color: #94a3b8;
  font-size: 13px;
}
.pipe-mini {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
}
.pipe-mini:hover {
  color: #2563eb;
}
h3 {
  margin: 0 0 16px;
  font-size: 16px;
}
</style>
