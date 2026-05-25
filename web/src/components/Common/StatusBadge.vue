<template>
  <el-tag :type="tagType" size="small" effect="light">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status?: string }>()

const tagType = computed(() => {
  const s = (props.status || '').toUpperCase()
  if (['SUCCESS', 'ACTIVE', 'CONNECTED'].includes(s)) return 'success'
  if (['RUNNING', 'PENDING'].includes(s)) return s === 'PENDING' ? 'warning' : 'primary'
  if (['FAILED', 'ERROR'].includes(s)) return 'danger'
  if (['CANCELLED', 'DRAFT', 'INACTIVE'].includes(s)) return 'info'
  return 'info'
})

const label = computed(() => props.status || '—')
</script>
