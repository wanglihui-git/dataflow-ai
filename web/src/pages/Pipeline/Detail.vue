<template>
  <div class="page-container" v-loading="loading">
    <PageHeader v-if="pipeline" :title="pipeline.name" :subtitle="pipeline.description">
      <StatusBadge :status="pipeline.status" />
      <el-select v-model="versionLabel" style="width: 200px" disabled>
        <el-option :label="versionLabel" :value="versionLabel" />
      </el-select>
      <el-button v-if="auth.canWrite" @click="router.push(`/pipelines/${id}/edit`)">编辑</el-button>
      <el-button v-if="auth.canWrite" @click="handleClone">克隆</el-button>
      <el-button type="success" @click="handleRun">运行</el-button>
      <el-button v-if="auth.canWrite" type="danger" @click="handleDelete">删除</el-button>
    </PageHeader>

    <el-row :gutter="16" v-if="pipeline">
      <el-col :span="10">
        <div class="card-panel">
          <h3>概要</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="权限">{{ pipeline.permissionLevel }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ pipeline.ownerId }}</el-descriptions-item>
            <el-descriptions-item label="创建">{{ pipeline.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="更新">{{ pipeline.updatedAt }}</el-descriptions-item>
          </el-descriptions>
          <el-button type="primary" class="editor-cta" @click="router.push(`/pipelines/${id}/edit`)">
            打开可视化编辑器
          </el-button>
        </div>
        <div class="card-panel" v-if="stats">
          <h3>执行统计</h3>
          <el-statistic title="总次数" :value="stats.total" />
          <el-statistic title="成功率" :value="(stats.successRate * 100).toFixed(1) + '%'" />
        </div>
      </el-col>
      <el-col :span="14">
        <div class="card-panel">
          <h3>最近运行</h3>
          <el-timeline v-if="runs.length">
            <el-timeline-item v-for="r in runs" :key="r.id" :timestamp="r.startTime">
              <StatusBadge :status="r.status" />
              <el-button link @click="router.push(`/executions/${r.id}`)">查看</el-button>
            </el-timeline-item>
          </el-timeline>
          <EmptyState v-else title="暂无运行记录" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as pipelineApi from '@/api/pipeline'
import * as executionApi from '@/api/execution'
import { useAuthStore } from '@/stores/auth'
import type { ExecutionRun, Pipeline } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const id = route.params.id as string
const loading = ref(false)
const pipeline = ref<Pipeline | null>(null)
const runs = ref<ExecutionRun[]>([])
const stats = ref<Record<string, number> | null>(null)

const versionLabel = computed(() => `当前版本 · ${pipeline.value?.updatedAt || '—'}`)

async function load() {
  loading.value = true
  try {
    pipeline.value = await pipelineApi.getPipeline(id)
    runs.value = (await pipelineApi.listPipelineRuns(id)).slice(0, 10)
    stats.value = await executionApi.getPipelineStats(id)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRun() {
  const run = await pipelineApi.runPipeline(id)
  router.push(`/executions/${run.id}`)
}

async function handleClone() {
  if (!pipeline.value) return
  const p = pipeline.value
  const created = await pipelineApi.createPipeline({
    name: `${p.name}-copy`,
    description: p.description,
    source: p.source,
    transforms: p.transforms,
    sink: p.sink,
    schedule: p.schedule,
    permissionLevel: p.permissionLevel
  })
  ElMessage.success('已克隆')
  router.push(`/pipelines/${created.id}/edit`)
}

async function handleDelete() {
  await ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' })
  await pipelineApi.deletePipeline(id)
  router.push('/pipelines')
}

onMounted(load)
</script>

<style scoped>
.editor-cta {
  margin-top: 16px;
  width: 100%;
}
h3 {
  margin: 0 0 12px;
}
</style>
