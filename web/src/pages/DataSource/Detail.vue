<template>
  <div class="page-container" v-loading="loading">
    <PageHeader v-if="ds" :title="ds.name">
      <StatusBadge :status="lastTestStatus" />
      <el-button @click="handleTest">测试连接</el-button>
      <el-button v-if="auth.canWrite" type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </PageHeader>

    <el-tabs v-if="ds" v-model="tab">
      <el-tab-pane label="基本配置" name="config">
        <div class="card-panel">
          <el-form label-width="120px">
            <el-form-item label="名称">
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="类型">
              <el-tag>{{ ds.type }}</el-tag>
            </el-form-item>
            <el-form-item label="连接配置">
              <el-input v-model="configJson" type="textarea" :rows="10" />
              <p class="hint">敏感字段保存后由服务端加密，不会明文回显</p>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
      <el-tab-pane label="连接测试" name="test">
        <div class="card-panel">
          <el-timeline>
            <el-timeline-item v-for="(h, i) in testHistory" :key="i" :timestamp="h.time">
              <StatusBadge :status="h.ok ? 'SUCCESS' : 'FAILED'" />
              {{ h.message }}
            </el-timeline-item>
          </el-timeline>
          <EmptyState v-if="!testHistory.length" title="暂无测试记录" description="点击测试连接" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="数据预览" name="preview">
        <div class="card-panel">
          <el-button type="primary" @click="handlePreview">采样预览</el-button>
          <pre v-if="previewResult" class="preview-json">{{ previewResult }}</pre>
        </div>
      </el-tab-pane>
      <el-tab-pane label="列权限" name="col">
        <div class="card-panel">
          <el-button size="small" @click="loadColPerms">刷新</el-button>
          <el-table :data="colPerms" style="margin-top: 12px">
            <el-table-column prop="id" label="ID" />
            <el-table-column prop="columnName" label="列名" />
            <el-table-column prop="permissionType" label="类型" />
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="行权限" name="row">
        <div class="card-panel">
          <el-button size="small" @click="loadRowPerms">刷新</el-button>
          <el-table :data="rowPerms" style="margin-top: 12px">
            <el-table-column prop="id" label="ID" />
            <el-table-column prop="filterCondition" label="条件" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as dataSourceApi from '@/api/dataSource'
import { useAuthStore } from '@/stores/auth'
import type { DataSource } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import StatusBadge from '@/components/Common/StatusBadge.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id as string
const loading = ref(false)
const saving = ref(false)
const ds = ref<DataSource | null>(null)
const tab = ref('config')
const configJson = ref('{}')
const form = reactive({ name: '' })
const lastTestStatus = ref('unknown')
const testHistory = ref<{ time: string; ok: boolean; message: string }[]>([])
const previewResult = ref('')
const colPerms = ref<Record<string, unknown>[]>([])
const rowPerms = ref<Record<string, unknown>[]>([])

async function load() {
  loading.value = true
  try {
    ds.value = await dataSourceApi.getDataSource(id)
    form.name = ds.value.name
    configJson.value = JSON.stringify(ds.value.connectionConfig || {}, null, 2)
    const key = `ds_test_${id}`
    const raw = sessionStorage.getItem(key)
    if (raw) testHistory.value = JSON.parse(raw)
  } finally {
    loading.value = false
  }
}

function pushTestHistory(ok: boolean, message: string) {
  testHistory.value.unshift({
    time: new Date().toLocaleString(),
    ok,
    message
  })
  sessionStorage.setItem(`ds_test_${id}`, JSON.stringify(testHistory.value.slice(0, 20)))
  lastTestStatus.value = ok ? 'SUCCESS' : 'FAILED'
}

async function handleTest() {
  try {
    const res = await dataSourceApi.testConnection(id)
    pushTestHistory(!!res.success, (res.message as string) || '测试完成')
    ElMessage.success('测试完成')
  } catch (e) {
    pushTestHistory(false, e instanceof Error ? e.message : '失败')
  }
}

async function handleSave() {
  saving.value = true
  try {
    let connectionConfig
    try {
      connectionConfig = JSON.parse(configJson.value)
    } catch {
      ElMessage.error('JSON 无效')
      return
    }
    await dataSourceApi.updateDataSource(id, { name: form.name, connectionConfig })
    ElMessage.success('已保存')
    load()
  } finally {
    saving.value = false
  }
}

async function handlePreview() {
  const res = await dataSourceApi.previewData(id)
  previewResult.value = JSON.stringify(res, null, 2)
}

async function loadColPerms() {
  colPerms.value = (await dataSourceApi.listColumnPermissions(id)) as Record<string, unknown>[]
}
async function loadRowPerms() {
  rowPerms.value = (await dataSourceApi.listRowPermissions(id)) as Record<string, unknown>[]
}

onMounted(load)
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #94a3b8;
}
.preview-json {
  margin-top: 12px;
  background: #f8fafc;
  padding: 12px;
  max-height: 400px;
  overflow: auto;
  font-size: 12px;
}
</style>
