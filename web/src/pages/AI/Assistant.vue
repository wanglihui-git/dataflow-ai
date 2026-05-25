<template>
  <div class="page-container">
    <PageHeader title="AI 助手" subtitle="自然语言生成转换节点与相似指令检索" />

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="card-panel chat-panel">
          <div class="messages">
            <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
              <strong>{{ msg.role === 'user' ? '你' : 'AI' }}：</strong>
              <pre v-if="msg.content.startsWith('{')">{{ msg.content }}</pre>
              <span v-else>{{ msg.content }}</span>
            </div>
          </div>
          <el-input
            v-model="instruction"
            type="textarea"
            :rows="3"
            placeholder="描述你希望的数据转换，例如：将金额字段保留两位小数并过滤空值..."
            @keydown.ctrl.enter="sendGenerate"
          />
          <div class="actions">
            <el-button type="primary" :loading="ai.loading" @click="sendGenerate">生成转换 (Ctrl+Enter)</el-button>
            <el-button :loading="ai.loading" @click="sendSearch">相似指令</el-button>
            <el-select v-model="pipelineId" placeholder="关联 Pipeline（可选）" clearable style="width: 220px">
              <el-option v-for="p in pipelines" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="card-panel">
          <h3>生成结果</h3>
          <pre v-if="ai.lastNodes.length" class="result-json">{{ JSON.stringify(ai.lastNodes, null, 2) }}</pre>
          <EmptyState v-else title="暂无生成结果" />
          <el-button-group v-if="ai.lastAiHelperId" style="margin-top: 12px">
            <el-button size="small" @click="feedback(1)">采纳</el-button>
            <el-button size="small" @click="feedback(0)">拒绝</el-button>
            <el-button size="small" @click="feedback(-1)">修改后采纳</el-button>
          </el-button-group>
        </div>
        <div class="card-panel">
          <h3>相似指令</h3>
          <ul v-if="ai.similarResults.length">
            <li v-for="(s, i) in ai.similarResults" :key="i">{{ formatSimilar(s) }}</li>
          </ul>
          <EmptyState v-else title="暂无相似结果" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAiStore } from '@/stores/ai'
import * as pipelineApi from '@/api/pipeline'
import type { Pipeline } from '@/types'
import PageHeader from '@/components/Common/PageHeader.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const ai = useAiStore()
const instruction = ref('')
const pipelineId = ref<string>()
const pipelines = ref<Pipeline[]>([])
const messages = ref<{ role: 'user' | 'assistant'; content: string }[]>([])

function formatSimilar(s: unknown) {
  if (typeof s === 'object' && s && 'instruction' in s) return (s as { instruction: string }).instruction
  return JSON.stringify(s)
}

async function sendGenerate() {
  if (!instruction.value.trim()) return
  messages.value.push({ role: 'user', content: instruction.value })
  try {
    const res = await ai.generate({
      instruction: instruction.value,
      pipelineId: pipelineId.value
    })
    const text = JSON.stringify(res, null, 2)
    messages.value.push({ role: 'assistant', content: text })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '生成失败')
  }
}

async function sendSearch() {
  try {
    await ai.searchSimilar(instruction.value)
    ElMessage.success('检索完成')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '检索失败')
  }
}

async function feedback(type: number) {
  if (!ai.lastAiHelperId) return
  await ai.feedback(ai.lastAiHelperId, type)
  ElMessage.success('感谢反馈')
}

onMounted(async () => {
  const res = await pipelineApi.listPipelines({ page: 0, size: 100 })
  pipelines.value = res.content
  const saved = localStorage.getItem('ai_chat_history')
  if (saved) messages.value = JSON.parse(saved)
})

watch(
  messages,
  (m) => localStorage.setItem('ai_chat_history', JSON.stringify(m.slice(-50))),
  { deep: true }
)
</script>

<style scoped>
.chat-panel {
  min-height: 480px;
  display: flex;
  flex-direction: column;
}
.messages {
  flex: 1;
  min-height: 280px;
  max-height: 400px;
  overflow: auto;
  margin-bottom: 12px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 6px;
}
.msg {
  margin-bottom: 12px;
  font-size: 14px;
}
.msg.user {
  color: #1e40af;
}
.msg.assistant pre {
  white-space: pre-wrap;
  font-size: 12px;
  margin: 4px 0 0;
}
.actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.result-json {
  background: #f1f5f9;
  padding: 12px;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
}
h3 {
  margin: 0 0 12px;
}
</style>
