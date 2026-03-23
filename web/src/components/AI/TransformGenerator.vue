<template>
  <div class="transform-generator">
    <div class="generator-header">
      <h3>AI 智能生成转换</h3>
      <p class="generator-desc">用自然语言描述您的数据转换需求</p>
    </div>

    <!-- 输入区域 -->
    <div class="input-section">
      <el-input
        v-model="instruction"
        type="textarea"
        :rows="4"
        placeholder="例如：将用户表中的 username 转换为大写，并根据 age 字段过滤出 18 岁以上的用户"
        @keydown.enter.ctrl="handleGenerate"
      />
      <div class="input-actions">
        <el-button type="primary" :loading="loading" @click="handleGenerate">
          <el-icon><MagicStick /></el-icon>
          生成转换
        </el-button>
        <el-button @click="handleClear">
          清空
        </el-button>
      </div>
    </div>

    <!-- 上下文配置 -->
    <el-collapse v-model="activeCollapse">
      <el-collapse-item title="上下文配置（可选）" name="context">
        <div class="context-config">
          <el-form label-width="100px" label-position="left">
            <el-form-item label="源数据 Schema">
              <el-input
                v-model="sourceSchemaStr"
                type="textarea"
                :rows="3"
                placeholder='[{"name": "username", "type": "string"}, {"name": "age", "type": "number"}]'
              />
            </el-form-item>
            <el-form-item label="目标数据 Schema">
              <el-input
                v-model="targetSchemaStr"
                type="textarea"
                :rows="3"
                placeholder='[{"name": "userName", "type": "string"}, {"name": "isAdult", "type": "boolean"}]'
              />
            </el-form-item>
          </el-form>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 生成结果 -->
    <div v-if="generatedTransforms.length" class="result-section">
      <div class="result-header">
        <span>生成结果</span>
        <el-button type="primary" size="small" @click="handleApplyAll">
          一键应用全部
        </el-button>
      </div>
      <div class="transform-list">
        <div
          v-for="(transform, index) in generatedTransforms"
          :key="index"
          class="transform-item"
        >
          <div class="transform-info">
            <el-tag size="small">{{ transform.type }}</el-tag>
            <span class="transform-name">{{ transform.name }}</span>
          </div>
          <div class="transform-config">
            <pre>{{ JSON.stringify(transform.config, null, 2) }}</pre>
          </div>
          <div class="transform-actions">
            <el-button type="primary" size="small" @click="handleApplyOne(index)">
              应用
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 相似指令 -->
    <div v-if="similarInstructions.length" class="similar-section">
      <div class="similar-header">
        <span>相似指令</span>
      </div>
      <div class="similar-list">
        <div
          v-for="item in similarInstructions"
          :key="item.id"
          class="similar-item"
          @click="handleUseSimilar(item)"
        >
          <span class="similar-instruction">{{ item.instruction }}</span>
          <span class="similar-similarity">{{ Math.round(item.similarity * 100) }}% 相似</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import type { Transform, SimilarInstruction, GenerateContext } from '@/types'
import { useAIStore } from '@/stores/ai'

const aiStore = useAIStore()

const instruction = ref('')
const loading = ref(false)

// 上下文配置
const activeCollapse = ref<string[]>(['context'])
const sourceSchemaStr = ref('')
const targetSchemaStr = ref('')

// 生成结果
const generatedTransforms = ref<Transform[]>([])
const similarInstructions = ref<SimilarInstruction[]>([])

const handleGenerate = async () => {
  if (!instruction.value.trim()) return

  loading.value = true
  try {
    // 构建上下文
    let context: GenerateContext | undefined
    try {
      if (sourceSchemaStr.value) {
        context = context || {}
        context.sourceSchema = JSON.parse(sourceSchemaStr.value)
      }
      if (targetSchemaStr.value) {
        context = context || {}
        context.targetSchema = JSON.parse(targetSchemaStr.value)
      }
    } catch {
      // JSON 解析错误，忽略
    }

    // 调用 AI 生成
    const transforms = await aiStore.generateTransforms(instruction.value, context)
    generatedTransforms.value = transforms

    // 同时搜索相似指令
    const similar = await aiStore.searchSimilar(instruction.value, 5)
    similarInstructions.value = similar
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  instruction.value = ''
  generatedTransforms.value = []
  similarInstructions.value = []
}

const handleApplyAll = () => {
  // TODO: 应用所有转换到编辑器
}

const handleApplyOne = (index: number) => {
  // TODO: 应用单个转换到编辑器
}

const handleUseSimilar = (item: SimilarInstruction) => {
  instruction.value = item.instruction
  if (item.transform) {
    generatedTransforms.value = [item.transform]
  }
}
</script>

<style scoped>
.transform-generator {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.generator-header h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}

.generator-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.input-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  gap: 8px;
}

.context-config {
  padding: 8px 0;
}

.result-section,
.similar-section {
  border-top: 1px solid var(--border-color-light);
  padding-top: 16px;
}

.result-header,
.similar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
}

.transform-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.transform-item {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

.transform-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.transform-name {
  font-weight: 500;
}

.transform-config {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 8px;
  margin-bottom: 8px;
}

.transform-config pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 12px;
  white-space: pre-wrap;
}

.transform-actions {
  text-align: right;
}

.similar-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.similar-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.similar-item:hover {
  background: var(--bg-secondary);
}

.similar-instruction {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.similar-similarity {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-left: 12px;
}
</style>