<template>
  <span :class="['permission-badge', `permission-badge--${type}`]">
    <el-icon v-if="icon" class="permission-badge__icon">
      <component :is="icon" />
    </el-icon>
    <span class="permission-badge__text">{{ text || label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Lock, Key, View, Warning } from '@element-plus/icons-vue'

interface Props {
  permission: string
  showIcon?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showIcon: true
})

const type = computed(() => {
  const typeMap: Record<string, string> = {
    FULL: 'success',
    MASKED: 'warning',
    NONE: 'danger',
    PRIVATE: 'secondary',
    SHARED: 'info',
    PUBLIC: 'success'
  }
  return typeMap[props.permission] || 'secondary'
})

const label = computed(() => {
  const labelMap: Record<string, string> = {
    FULL: '完全访问',
    MASKED: '脱敏访问',
    NONE: '无权限',
    PRIVATE: '私有',
    SHARED: '共享',
    PUBLIC: '公开'
  }
  return labelMap[props.permission] || props.permission
})

const text = computed(() => props.permission)

const icon = computed(() => {
  if (!props.showIcon) return null
  const iconMap: Record<string, any> = {
    FULL: Key,
    MASKED: Warning,
    NONE: Lock,
    PRIVATE: Lock,
    SHARED: View,
    PUBLIC: Key
  }
  return iconMap[props.permission]
})
</script>

<style scoped>
.permission-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.permission-badge--success {
  background-color: #e6f7ed;
  color: #1d7a3e;
}

.permission-badge--warning {
  background-color: #fff7e6;
  color: #b76e00;
}

.permission-badge--danger {
  background-color: #ffeaea;
  color: #c53030;
}

.permission-badge--info {
  background-color: #e6f0ff;
  color: #1a5ce6;
}

.permission-badge--secondary {
  background-color: #f1f3f5;
  color: #495057;
}

.permission-badge__icon {
  font-size: 12px;
}
</style>