<template>
  <div class="log-viewer">
    <div class="log-toolbar">
      <el-select v-model="levelFilter" placeholder="日志级别" clearable style="width: 120px">
        <el-option label="全部" value="" />
        <el-option label="INFO" value="INFO" />
        <el-option label="WARN" value="WARN" />
        <el-option label="ERROR" value="ERROR" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索日志内容"
        prefix-icon="Search"
        clearable
        style="width: 200px"
      />
      <el-checkbox v-model="autoScroll">自动滚动</el-checkbox>
    </div>

    <div ref="logContainer" class="log-content">
      <div
        v-for="(log, index) in filteredLogs"
        :key="index"
        :class="['log-entry', `log-entry--${log.level.toLowerCase()}`]"
      >
        <span class="log-time">{{ formatTime(log.timestamp) }}</span>
        <span :class="['log-level', `log-level--${log.level.toLowerCase()}`]">
          {{ log.level }}
        </span>
        <span class="log-message">{{ log.message }}</span>
      </div>

      <EmptyState
        v-if="!filteredLogs.length"
        type="search"
        title="暂无日志"
        description="没有找到匹配的日志"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import type { LogEntry } from '@/types'
import { useExecutionStore } from '@/stores/execution'
import EmptyState from '@/components/Common/EmptyState.vue'

interface Props {
  runId: string
}

const props = defineProps<Props>()

const executionStore = useExecutionStore()

const levelFilter = ref('')
const searchKeyword = ref('')
const autoScroll = ref(true)
const logContainer = ref<HTMLElement>()

const logs = computed(() => executionStore.executionLogs)

const filteredLogs = computed(() => {
  let result = logs.value

  if (levelFilter.value) {
    result = result.filter(log => log.level === levelFilter.value)
  }

  if (searchKeyword.value) {
    result = result.filter(log =>
      log.message.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  return result
})

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3
  })
}

const loadLogs = async () => {
  await executionStore.fetchExecutionLogs(props.runId)
  if (autoScroll.value) {
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
}

// 轮询刷新日志
let pollTimer: ReturnType<typeof setInterval> | null = null

const startPolling = () => {
  pollTimer = setInterval(loadLogs, 3000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(() => props.runId, () => {
  loadLogs()
  startPolling()
}, { immediate: true })

onMounted(() => {
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.log-viewer {
  display: flex;
  flex-direction: column;
  height: 300px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.log-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  background: #1e1e1e;
  font-family: var(--font-mono);
  font-size: 12px;
}

.log-entry {
  display: flex;
  gap: 12px;
  padding: 2px 0;
  color: #d4d4d4;
}

.log-entry--error {
  color: #f48771;
}

.log-entry--warn {
  color: #cca700;
}

.log-time {
  color: #858585;
  flex-shrink: 0;
}

.log-level {
  flex-shrink: 0;
  font-weight: 500;
}

.log-level--info {
  color: #4fc1ff;
}

.log-level--warn {
  color: #cca700;
}

.log-level--error {
  color: #f48771;
}

.log-message {
  flex: 1;
  word-break: break-all;
}
</style>