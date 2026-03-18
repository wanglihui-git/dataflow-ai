<template>
  <div class="data-source-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据源列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建数据源
          </el-button>
        </div>
      </template>

      <el-table :data="dataSources" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建者" width="150" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link size="small" @click="handleTestConnection(row.id)">
              测试连接
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建数据源对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建数据源" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="createForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRES" />
            <el-option label="API" value="API" />
            <el-option label="Kafka" value="KAFKA" />
            <el-option label="CSV" value="CSV" />
          </el-select>
        </el-form-item>
        <el-form-item label="连接配置">
          <el-input
            v-model="connectionConfigStr"
            type="textarea"
            :rows="4"
            placeholder='请输入 JSON 配置，例如: {"host": "localhost", "port": 3306}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DataSource } from '@/types'
import { dataSourceApi } from '@/api/dataSource'

const loading = ref(false)
const createLoading = ref(false)
const dataSources = ref<DataSource[]>([])
const createDialogVisible = ref(false)

const createForm = reactive({
  name: '',
  type: '' as DataSource['type'] | '',
  connectionConfig: {} as Record<string, unknown>
})

const connectionConfigStr = computed({
  get: () => JSON.stringify(createForm.connectionConfig, null, 2),
  set: (val: string) => {
    try {
      createForm.connectionConfig = val ? JSON.parse(val) : {}
    } catch {
      // 无效的 JSON，忽略
    }
  }
})

const loadDataSources = async () => {
  loading.value = true
  try {
    const res = await dataSourceApi.getList()
    dataSources.value = res.data
  } catch (err) {
    ElMessage.error('加载数据源列表失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleTestConnection = async (id: string) => {
  try {
    await dataSourceApi.testConnection(id)
    ElMessage.success('连接测试成功')
  } catch (err) {
    ElMessage.error('连接测试失败')
  }
}

const handleDelete = async (row: DataSource) => {
  try {
    await ElMessageBox.confirm(`确定要删除数据源 "${row.name}" 吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await dataSourceApi.delete(row.id)
    ElMessage.success('删除成功')
    loadDataSources()
  } catch (err: unknown) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleCreate = () => {
  createForm.name = ''
  createForm.type = ''
  createForm.connectionConfig = {}
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.name) {
    ElMessage.warning('请输入数据源名称')
    return
  }
  if (!createForm.type) {
    ElMessage.warning('请选择数据源类型')
    return
  }
  createLoading.value = true
  try {
    await dataSourceApi.create({
      name: createForm.name,
      type: createForm.type as DataSource['type'],
      connectionConfig: createForm.connectionConfig
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadDataSources()
  } catch (err) {
    ElMessage.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
