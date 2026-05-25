<template>
  <div class="page-container">
    <PageHeader title="数据源" subtitle="管理连接与凭证">
      <el-button v-if="auth.canWrite" type="primary" @click="showCreate = true">添加数据源</el-button>
    </PageHeader>

    <div class="card-panel filters">
      <el-input v-model="keyword" placeholder="搜索名称" clearable style="width: 200px" />
      <el-select v-model="typeFilter" placeholder="类型" clearable style="width: 140px">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-button @click="load">查询</el-button>
    </div>

    <div class="card-panel" v-loading="loading">
      <el-table :data="filtered">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column label="连接状态" width="120">
          <template #default="{ row }">
            <StatusBadge :status="testStatus[row.id] || 'unknown'" />
          </template>
        </el-table-column>
        <el-table-column prop="ownerId" label="创建者" width="100" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button link @click="handleTest(row.id)">测试</el-button>
            <el-button link type="primary" @click="router.push(`/data-sources/${row.id}`)">编辑</el-button>
            <el-button link @click="handlePreview(row.id)">预览</el-button>
            <el-button v-if="auth.canWrite" link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showCreate" title="创建数据源" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="连接配置">
          <el-input v-model="configJson" type="textarea" :rows="6" placeholder='{"host":"localhost"}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as dataSourceApi from '@/api/dataSource'
import { useAuthStore } from '@/stores/auth'
import type { DataSource, DataSourceType } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'

const router = useRouter()
const auth = useAuthStore()
const types: DataSourceType[] = ['MYSQL', 'POSTGRES', 'API', 'KAFKA', 'CSV']
const loading = ref(false)
const items = ref<DataSource[]>([])
const testStatus = ref<Record<string, string>>({})
const keyword = ref('')
const typeFilter = ref('')
const showCreate = ref(false)
const configJson = ref('{}')
const form = reactive({ name: '', type: 'MYSQL' as DataSourceType })

const filtered = computed(() => {
  return items.value.filter((d) => {
    if (keyword.value && !d.name.includes(keyword.value)) return false
    if (typeFilter.value && d.type !== typeFilter.value) return false
    return true
  })
})

async function load() {
  loading.value = true
  try {
    const res = await dataSourceApi.listDataSources()
    items.value = res.content
  } finally {
    loading.value = false
  }
}

async function handleTest(id: string) {
  try {
    const res = await dataSourceApi.testConnection(id)
    testStatus.value[id] = (res.success as boolean) ? 'SUCCESS' : 'FAILED'
    ElMessage.success('测试完成')
  } catch {
    testStatus.value[id] = 'FAILED'
  }
}

async function handlePreview(id: string) {
  const res = await dataSourceApi.previewData(id, { sampleSize: 10 })
  ElMessageBox.alert(JSON.stringify(res, null, 2), '数据预览')
}

async function submitCreate() {
  let connectionConfig = {}
  try {
    connectionConfig = JSON.parse(configJson.value)
  } catch {
    ElMessage.error('连接配置 JSON 无效')
    return
  }
  await dataSourceApi.createDataSource({
    name: form.name,
    type: form.type,
    connectionConfig
  })
  showCreate.value = false
  ElMessage.success('已创建')
  load()
}

async function handleDelete(id: string) {
  await ElMessageBox.confirm('确定删除？', '警告', { type: 'warning' })
  await dataSourceApi.deleteDataSource(id)
  load()
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
