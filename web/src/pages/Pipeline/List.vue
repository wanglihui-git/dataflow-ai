<template>
  <div class="page-container">
    <PageHeader title="Pipeline" subtitle="浏览与管理数据管道">
      <el-button v-if="auth.canWrite" type="primary" @click="router.push('/pipelines/create')">
        创建管道
      </el-button>
    </PageHeader>

    <div class="card-panel filters">
      <el-input v-model="searchName" placeholder="搜索名称" clearable style="width: 220px" @clear="load" @keyup.enter="load" />
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 140px" @change="load">
        <el-option label="active" value="active" />
        <el-option label="draft" value="draft" />
      </el-select>
      <el-select v-model="filterPerm" placeholder="权限级别" clearable style="width: 140px" @change="load">
        <el-option label="PRIVATE" value="PRIVATE" />
        <el-option label="SHARED" value="SHARED" />
        <el-option label="PUBLIC" value="PUBLIC" />
      </el-select>
      <el-button @click="load">查询</el-button>
    </div>

    <div class="card-panel" v-loading="loading">
      <el-table :data="filtered" row-key="id">
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="permissionLevel" label="权限" width="100" />
        <el-table-column prop="ownerId" label="负责人" width="100" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="最后运行" width="120">
          <template #default="{ row }">{{ lastRunMap[row.id] || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/pipelines/${row.id}`)">查看</el-button>
            <el-button v-if="auth.canWrite" link @click="router.push(`/pipelines/${row.id}/edit`)">编辑</el-button>
            <el-button link type="success" @click="handleRun(row.id)">运行</el-button>
            <el-button v-if="auth.canWrite" link type="danger" @click="handleDelete(row.id)">删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as pipelineApi from '@/api/pipeline'
import { useAuthStore } from '@/stores/auth'
import type { Pipeline } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const items = ref<Pipeline[]>([])
const lastRunMap = ref<Record<string, string>>({})
const searchName = ref('')
const filterStatus = ref('')
const filterPerm = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const filtered = computed(() => {
  let list = items.value
  if (filterStatus.value) list = list.filter((p) => p.status === filterStatus.value)
  if (filterPerm.value) list = list.filter((p) => p.permissionLevel === filterPerm.value)
  return list
})

async function load() {
  loading.value = true
  try {
    const res = await pipelineApi.listPipelines({
      name: searchName.value || undefined,
      page: page.value - 1,
      size: size.value
    })
    items.value = res.content
    total.value = res.totalElements
    for (const p of res.content.slice(0, 10)) {
      try {
        const runs = await pipelineApi.listPipelineRuns(p.id)
        lastRunMap.value[p.id] = runs[0]?.status || '—'
      } catch {
        lastRunMap.value[p.id] = '—'
      }
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRun(id: string) {
  try {
    const run = await pipelineApi.runPipeline(id)
    ElMessage.success('已触发运行')
    router.push(`/executions/${run.id}`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '运行失败')
  }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm('确定删除该管道？', '警告', { type: 'warning' })
    await pipelineApi.deletePipeline(id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* cancel */
  }
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
