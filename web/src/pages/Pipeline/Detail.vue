<template>
  <div class="pipeline-detail">
    <el-page-header @back="goBack" content="Pipeline 详情" />

    <el-card v-loading="loading" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>{{ pipeline?.name }}</span>
          <div class="actions">
            <el-button type="primary" @click="handleExecute" :disabled="pipeline?.status === 'RUNNING'">
              <el-icon><VideoPlay /></el-icon>
              执行
            </el-button>
            <el-button @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ pipeline?.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(pipeline?.status || '')">{{ pipeline?.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ pipeline?.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权限级别">{{ pipeline?.permissionLevel }}</el-descriptions-item>
        <el-descriptions-item label="所有者ID">{{ pipeline?.ownerId }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(pipeline?.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(pipeline?.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Source 配置</span>
          </template>
          <pre class="config-json">{{ JSON.stringify(pipeline?.source, null, 2) }}</pre>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Transforms ({{ pipeline?.transforms?.length || 0 }})</span>
          </template>
          <pre class="config-json">{{ JSON.stringify(pipeline?.transforms, null, 2) }}</pre>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>Sink 配置</span>
          </template>
          <pre class="config-json">{{ JSON.stringify(pipeline?.sink, null, 2) }}</pre>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>执行记录</span>
      </template>
      <el-table :data="executionRuns" stripe size="small">
        <el-table-column prop="id" label="执行ID" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime ? formatDate(row.startTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime ? formatDate(row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Pipeline, ExecutionRun } from '@/types'
import { pipelineApi } from '@/api/pipeline'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const pipeline = ref<Pipeline | null>(null)
const executionRuns = ref<ExecutionRun[]>([])

const pipelineId = route.params.id as string

const loadPipeline = async () => {
  loading.value = true
  try {
    const res = await pipelineApi.getById(pipelineId)
    pipeline.value = res.data
  } catch (err) {
    ElMessage.error('加载 Pipeline 详情失败')
  } finally {
    loading.value = false
  }
}

const loadExecutionRuns = async () => {
  try {
    const res = await pipelineApi.getExecutionRuns(pipelineId)
    executionRuns.value = res.data
  } catch (err) {
    console.error('加载执行记录失败', err)
  }
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    RUNNING: 'success',
    SUCCESS: 'success',
    FAILED: 'danger',
    PENDING: 'warning',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const goBack = () => {
  router.push('/pipelines')
}

const handleExecute = async () => {
  try {
    await ElMessageBox.confirm('确定要执行此 Pipeline 吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await pipelineApi.execute(pipelineId)
    ElMessage.success('Pipeline 已启动执行')
    loadPipeline()
  } catch (err: unknown) {
    if (err !== 'cancel') {
      ElMessage.error('执行失败')
    }
  }
}

onMounted(() => {
  loadPipeline()
  loadExecutionRuns()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-json {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
  margin: 0;
}
</style>
