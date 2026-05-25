<template>
  <div class="empty-state">
    <el-icon :size="48" color="#94a3b8"><component :is="icon || iconComponent" /></el-icon>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <slot name="actions" />
  </div>
</template>

<script setup lang="ts">
import { Box } from '@element-plus/icons-vue'
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    icon?: Component
  }>(),
  { title: '暂无数据' }
)

const iconComponent = Box
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: #64748b;
}
.empty-state h3 {
  margin: 12px 0 8px;
  color: #334155;
}
</style>
