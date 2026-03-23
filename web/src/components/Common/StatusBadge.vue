<template>
  <span :class="['status-badge', `status-badge--${type}`, { 'status-badge--pulse': pulse }]">
    <span v-if="pulse" class="status-badge__dot"></span>
    <span class="status-badge__text">{{ text || label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: string
  pulse?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  pulse: false
})

const type = computed(() => {
  const statusMap: Record<string, string> = {
    PENDING: 'warning',
    RUNNING: 'info',
    SUCCESS: 'success',
    FAILED: 'danger',
    CANCELLED: 'secondary',
    ACTIVE: 'success',
    INACTIVE: 'secondary',
    DRAFT: 'secondary',
    PUBLISHED: 'success'
  }
  return statusMap[props.status] || 'secondary'
})

const label = computed(() => {
  const labelMap: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败',
    CANCELLED: '已取消',
    ACTIVE: '启用',
    INACTIVE: '停用',
    DRAFT: '草稿',
    PUBLISHED: '已发布'
  }
  return labelMap[props.status] || props.status
})

const text = computed(() => props.status)
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.status-badge--success {
  background-color: #e6f7ed;
  color: #1d7a3e;
}

.status-badge--warning {
  background-color: #fff7e6;
  color: #b76e00;
}

.status-badge--danger {
  background-color: #ffeaea;
  color: #c53030;
}

.status-badge--info {
  background-color: #e6f0ff;
  color: #1a5ce6;
}

.status-badge--secondary {
  background-color: #f1f3f5;
  color: #495057;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: currentColor;
}

.status-badge--pulse .status-badge__dot {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}
</style>