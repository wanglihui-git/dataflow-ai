<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="Pipeline 总数" :value="pipelineCount">
            <template #prefix>
              <el-icon class="statistic-icon"><Connection /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="数据源总数" :value="dataSourceCount">
            <template #prefix>
              <el-icon class="statistic-icon"><DataAnalysis /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <el-statistic title="运行中" :value="runningCount">
            <template #prefix>
              <el-icon class="statistic-icon"><VideoPlay /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快速操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="goToPipelines">
              <el-icon><Plus /></el-icon>
              新建 Pipeline
            </el-button>
            <el-button type="success" size="large" @click="goToDataSources">
              <el-icon><Plus /></el-icon>
              新建数据源
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pipelineApi } from '@/api/pipeline'
import { dataSourceApi } from '@/api/dataSource'

const router = useRouter()

const pipelineCount = ref(0)
const dataSourceCount = ref(0)
const runningCount = ref(0)

const loadData = async () => {
  try {
    const [pipelineRes, dataSourceRes] = await Promise.all([
      pipelineApi.getList(),
      dataSourceApi.getList()
    ])
    pipelineCount.value = pipelineRes.data.length
    dataSourceCount.value = dataSourceRes.data.length
    runningCount.value = pipelineRes.data.filter(p => p.status === 'RUNNING').length
  } catch (err) {
    console.error('加载统计数据失败', err)
  }
}

const goToPipelines = () => {
  router.push('/pipelines')
}

const goToDataSources = () => {
  router.push('/data-sources')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard h2 {
  margin: 0;
  color: #333;
}

.statistic-icon {
  color: #409EFF;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-actions {
  display: flex;
  gap: 16px;
}
</style>
