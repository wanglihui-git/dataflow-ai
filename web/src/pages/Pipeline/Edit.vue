<template>
  <div class="pipeline-edit-page">
    <PipelineEditor />
  </div>
</template>

<script setup lang="ts">
import PipelineEditor from '@/components/PipelineEditor/index.vue'
</script>

<style scoped>
.pipeline-edit-page {
  height: calc(100vh - 60px);
}
</style>