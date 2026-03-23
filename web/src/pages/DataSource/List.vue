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
            <el-tag>{{ typeLabels[row.type] || row.type }}</el-tag>
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
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handlePreview(row)">
              预览
            </el-button>
            <el-button type="success" link size="small" @click="handleTestConnection(row.id)">
              测试
            </el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建数据源对话框 -->
    <CreateDialog
      v-model="createDialogVisible"
      @success="handleCreateSuccess"
    />

    <!-- 编辑数据源对话框 -->
    <EditDialog
      v-model="editDialogVisible"
      :data-source="currentDataSource"
      @success="handleEditSuccess"
    />

    <!-- 预览对话框 -->
    <PreviewDialog
      v-model="previewDialogVisible"
      :data-source-id="currentDataSourceId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { DataSource } from '@/types'
import { dataSourceApi } from '@/api/dataSource'
import CreateDialog from '@/components/DataSource/CreateDialog.vue'
import EditDialog from '@/components/DataSource/EditDialog.vue'
import PreviewDialog from '@/components/DataSource/PreviewDialog.vue'

const typeLabels: Record<string, string> = {
  MYSQL: 'MySQL',
  POSTGRES: 'PostgreSQL',
  API: 'API',
  KAFKA: 'Kafka',
  CSV: 'CSV'
}

const loading = ref(false)
const dataSources = ref<DataSource[]>([])

// 新建对话框
const createDialogVisible = ref(false)

// 编辑对话框
const editDialogVisible = ref(false)
const currentDataSource = ref<DataSource | null>(null)

// 预览对话框
const previewDialogVisible = ref(false)
const currentDataSourceId = ref('')

const loadDataSources = async () => {
  loading.value = true
  try {
    const res = await dataSourceApi.getList()
    dataSources.value = res.data.data || []
  } catch (err) {
    ElMessage.error('加载数据源列表失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleCreate = () => {
  createDialogVisible.value = true
}

const handleCreateSuccess = () => {
  loadDataSources()
}

const handleEdit = (row: DataSource) => {
  currentDataSource.value = row
  editDialogVisible.value = true
}

const handleEditSuccess = () => {
  loadDataSources()
}

const handlePreview = (row: DataSource) => {
  currentDataSourceId.value = row.id
  previewDialogVisible.value = true
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