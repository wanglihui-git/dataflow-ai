<template>
  <el-dialog
    v-model="visible"
    title="创建数据源"
    width="640px"
    :close-on-click-modal="false"
    @closed="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="数据源名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入数据源名称"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="数据源类型" prop="type">
        <el-select v-model="formData.type" placeholder="请选择数据源类型" style="width: 100%">
          <el-option
            v-for="type in dataSourceTypes"
            :key="type.value"
            :label="type.label"
            :value="type.value"
          >
            <span>{{ type.label }}</span>
            <span class="type-description">{{ type.description }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="连接配置" prop="connectionConfig">
        <ConnectionForm
          v-model="formData.connectionConfig"
          :type="formData.type"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        创建
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import type { DataSource } from '@/types'
import { dataSourceApi } from '@/api/dataSource'
import ConnectionForm from './ConnectionForm.vue'

interface DataSourceTypeOption {
  value: string
  label: string
  description: string
}

const dataSourceTypes: DataSourceTypeOption[] = [
  { value: 'MYSQL', label: 'MySQL', description: 'MySQL 数据库' },
  { value: 'POSTGRES', label: 'PostgreSQL', description: 'PostgreSQL 数据库' },
  { value: 'API', label: 'API', description: 'HTTP API 接口' },
  { value: 'KAFKA', label: 'Kafka', description: 'Kafka 消息队列' },
  { value: 'CSV', label: 'CSV', description: 'CSV 本地文件' }
]

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', dataSource: DataSource): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref<FormInstance>()
const loading = ref(false)

const formData = reactive({
  name: '',
  type: 'MYSQL' as string,
  connectionConfig: {} as Record<string, unknown>
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入数据源名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择数据源类型', trigger: 'change' }
  ],
  connectionConfig: [
    { required: true, message: '请填写连接配置', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return // 验证失败，阻止提交
  }

  loading.value = true
  try {
    const response = await dataSourceApi.create(formData as Partial<DataSource>)
    const dataSource = response.data
    if (dataSource) {
      ElMessage.success('创建成功')
      emit('success', dataSource)
      handleClose()
    }
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
  formData.connectionConfig = {}
  visible.value = false
}
</script>

<style scoped>
.type-description {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>