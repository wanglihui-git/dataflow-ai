<template>
  <el-button
    :type="buttonType"
    :loading="loading"
    :disabled="disabled"
    :size="size"
    @click="handleTest"
  >
    <el-icon v-if="!loading"><Connection /></el-icon>
    <slot>{{ text }}</slot>
  </el-button>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection } from '@element-plus/icons-vue'
import type { ConnectionTestResult } from '@/types'
import { dataSourceApi } from '@/api/dataSource'

interface Props {
  dataSourceId: string
  text?: string
  size?: 'large' | 'default' | 'small'
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'text'
}

const props = withDefaults(defineProps<Props>(), {
  text: '测试连接',
  size: 'default',
  type: 'primary'
})

const loading = ref(false)
const disabled = computed(() => !props.dataSourceId)

const buttonType = computed(() => props.type)

const handleTest = async () => {
  if (!props.dataSourceId || loading.value) return

  loading.value = true
  try {
    const result = await dataSourceApi.testConnection(props.dataSourceId)
    const testResult = result as unknown as ConnectionTestResult

    if (testResult?.success) {
      ElMessage.success(testResult.message || '连接成功')
    } else {
      ElMessage.warning(testResult?.message || '连接失败')
    }
  } catch (error) {
    ElMessage.error('测试连接失败')
  } finally {
    loading.value = false
  }
}
</script>