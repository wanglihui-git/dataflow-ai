<template>
  <div class="stats-chart">
    <div class="chart-header">
      <span class="chart-title">{{ title }}</span>
      <el-select v-model="timeRange" size="small" style="width: 120px">
        <el-option label="最近 7 天" value="7d" />
        <el-option label="最近 30 天" value="30d" />
        <el-option label="最近 90 天" value="90d" />
      </el-select>
    </div>
    <div v-loading="loading" class="chart-content">
      <div v-if="!loading && !chartData.length" class="empty-chart">
        暂无数据
      </div>
      <div v-else ref="chartRef" class="echart-container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import type { ExecutionStats } from '@/types'

interface Props {
  title: string
  pipelineId?: string
}

const props = defineProps<Props>()

const loading = ref(false)
const timeRange = ref('7d')
const chartRef = ref<HTMLElement>()
let chartInstance: ECharts | null = null

const chartData = ref<Array<{ date: string; count: number; duration: number }>>([])

const loadStats = async () => {
  if (!props.pipelineId) return

  loading.value = true
  try {
    // TODO: 从 API 获取统计数据
    // const stats = await executionStore.fetchExecutionStats(props.pipelineId, timeRange.value)
    // chartData.value = stats?.recentTrends || []
  } finally {
    loading.value = false
  }
}

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['执行次数', '平均耗时(秒)'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.value.map(d => d.date),
      axisLine: {
        lineStyle: {
          color: '#e9ecef'
        }
      },
      axisLabel: {
        color: '#868e96'
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '执行次数',
        axisLine: {
          show: false
        },
        axisLabel: {
          color: '#868e96'
        }
      },
      {
        type: 'value',
        name: '耗时(秒)',
        axisLine: {
          show: false
        },
        axisLabel: {
          color: '#868e96'
        }
      }
    ],
    series: [
      {
        name: '执行次数',
        type: 'bar',
        data: chartData.value.map(d => d.count),
        itemStyle: {
          color: '#1971c2'
        }
      },
      {
        name: '平均耗时(秒)',
        type: 'line',
        yAxisIndex: 1,
        data: chartData.value.map(d => d.duration),
        itemStyle: {
          color: '#2f9e44'
        },
        smooth: true
      }
    ]
  }

  chartInstance.setOption(option)
}

const resizeChart = () => {
  chartInstance?.resize()
}

watch(timeRange, loadStats)

watch(chartData, updateChart)

onMounted(() => {
  loadStats()
  initChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
})
</script>

<style scoped>
.stats-chart {
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-weight: 600;
}

.chart-content {
  height: 300px;
}

.echart-container {
  width: 100%;
  height: 100%;
}

.empty-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-tertiary);
}
</style>