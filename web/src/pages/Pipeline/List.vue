<template>
  <div class="pipeline-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Pipeline 列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建 Pipeline
          </el-button>
        </div>
      </template>

      <el-table :data="pipelines" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permissionLevel" label="权限级别" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row.id)">
              详情
            </el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row.id)">
              编辑
            </el-button>
            <el-button type="success" link size="small" @click="handleExecute(row.id)">
              执行
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 Pipeline 对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建 Pipeline" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="请输入 Pipeline 名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" placeholder="请输入描述" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Pipeline } from '@/types'
import { pipelineApi } from '@/api/pipeline'

const router = useRouter()

const loading = ref(false)
const createLoading = ref(false)
const pipelines = ref<Pipeline[]>([])
const createDialogVisible = ref(false)

const createForm = reactive({
  name: '',
  description: ''
})

const loadPipelines = async () => {
  loading.value = true
  try {
    const res = await pipelineApi.getList()
    pipelines.value = res.data
  } catch (err) {
    ElMessage.error('加载 Pipeline 列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    RUNNING: 'success',
    SUCCESS: 'success',
    FAILED: 'danger',
    PENDING: 'warning'
  }
  return map[status] || 'info'
}

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleDetail = (id: string) => {
  router.push(`/pipelines/${id}`)
}

const handleEdit = (id: string) => {
  router.push(`/pipelines/${id}/edit`)
}

const handleExecute = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定要执行此 Pipeline 吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await pipelineApi.run(id)
    ElMessage.success('Pipeline 已启动执行')
    loadPipelines()
  } catch (err: unknown) {
    if (err !== 'cancel') {
      ElMessage.error('执行失败')
    }
  }
}

const handleDelete = async (row: Pipeline) => {
  try {
    await ElMessageBox.confirm(`确定要删除 Pipeline "${row.name}" 吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await pipelineApi.delete(row.id)
    ElMessage.success('删除成功')
    loadPipelines()
  } catch (err: unknown) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleCreate = () => {
  createForm.name = ''
  createForm.description = ''
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createForm.name) {
    ElMessage.warning('请输入 Pipeline 名称')
    return
  }
  createLoading.value = true
  try {
    await pipelineApi.create({
      name: createForm.name,
      description: createForm.description,
      source: { type: 'manual' },
      transforms: [],
      sink: { type: 'database' },
      permissionLevel: 'PRIVATE'
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    loadPipelines()
  } catch (err) {
    ElMessage.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

onMounted(() => {
  loadPipelines()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
