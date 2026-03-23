<template>
  <div class="ai-assistant-page">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <TransformGenerator />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>历史记录</span>
          </template>
          <div class="history-list">
            <div
              v-for="item in instructionHistory"
              :key="item.id"
              class="history-item"
              @click="handleUseHistory(item)"
            >
              <div class="history-instruction">{{ item.instruction }}</div>
              <div class="history-meta">
                <span>{{ item.transforms.length }} 个转换</span>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
            </div>
            <EmptyState
              v-if="!instructionHistory.length"
              type="search"
              title="暂无历史"
              description="您的 AI 生成历史将显示在这里"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAIStore } from '@/stores/ai'
import TransformGenerator from '@/components/AI/TransformGenerator.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const aiStore = useAIStore()

const instructionHistory = computed(() => aiStore.instructionHistory)

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const handleUseHistory = (item: { instruction: string }) => {
  // TODO: 使用历史记录
}
</script>

<style scoped>
.ai-assistant-page {
  padding: 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 500px;
  overflow-y: auto;
}

.history-item {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.history-item:hover {
  background: var(--bg-secondary);
}

.history-instruction {
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>