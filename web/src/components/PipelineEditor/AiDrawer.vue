<template>
  <el-drawer v-model="visible" title="AI 辅助" size="420px" direction="rtl">
    <el-input
      v-model="instruction"
      type="textarea"
      :rows="4"
      placeholder="用自然语言描述转换需求..."
    />
    <div class="drawer-actions">
      <el-button type="primary" :loading="ai.loading" @click="handleGenerate">生成节点</el-button>
      <el-button :loading="ai.loading" @click="handleSearch">相似指令</el-button>
    </div>
    <div v-if="ai.lastNodes.length" class="preview">
      <h4>生成预览</h4>
      <pre>{{ JSON.stringify(ai.lastNodes, null, 2) }}</pre>
      <el-button type="success" @click="emit('apply', ai.lastNodes)">应用到画布</el-button>
      <el-button-group class="feedback">
        <el-button size="small" @click="feedback(1)">采纳</el-button>
        <el-button size="small" @click="feedback(0)">拒绝</el-button>
      </el-button-group>
    </div>
    <div v-if="ai.similarResults.length" class="preview">
      <h4>相似指令</h4>
      <ul>
        <li v-for="(s, i) in ai.similarResults" :key="i">{{ JSON.stringify(s) }}</li>
      </ul>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAiStore } from '@/stores/ai'

const props = defineProps<{ modelValue: boolean; pipelineId?: string }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'apply', nodes: Record<string, unknown>[]): void
}>()

const ai = useAiStore()
const visible = ref(props.modelValue)
const instruction = ref('')

watch(
  () => props.modelValue,
  (v) => (visible.value = v)
)
watch(visible, (v) => emit('update:modelValue', v))

async function handleGenerate() {
  if (!instruction.value.trim()) return
  try {
    await ai.generate({
      instruction: instruction.value,
      pipelineId: props.pipelineId
    })
    ElMessage.success('生成完成')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '生成失败')
  }
}

async function handleSearch() {
  try {
    await ai.searchSimilar(instruction.value)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '检索失败')
  }
}

async function feedback(type: number) {
  if (!ai.lastAiHelperId) return
  await ai.feedback(ai.lastAiHelperId, type)
  ElMessage.success('感谢反馈')
}
</script>

<style scoped>
.drawer-actions {
  margin: 12px 0;
  display: flex;
  gap: 8px;
}
.preview {
  margin-top: 16px;
}
.preview pre {
  background: #f1f5f9;
  padding: 8px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}
.feedback {
  margin-top: 8px;
}
</style>
