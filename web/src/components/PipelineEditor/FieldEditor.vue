<template>
  <el-dialog
    v-model="visible"
    title="字段编辑"
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="formData" label-position="top">
      <!-- 源字段和目标字段 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="源数据字段">
            <el-input :model-value="formData.sourcePath" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="目标数据字段" prop="targetName">
            <el-input v-model="formData.targetName" placeholder="请输入目标字段名" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 条件判断开关 -->
      <el-form-item>
        <el-switch v-model="formData.conditionEnabled" />
        <span class="switch-label">启用条件判断</span>
      </el-form-item>

      <!-- 条件表达式编辑 -->
      <template v-if="formData.conditionEnabled">
        <el-divider content-position="left">条件分支</el-divider>
        <div class="condition-branches">
          <div
            v-for="(branch, index) in formData.conditions"
            :key="branch.id"
            class="condition-branch"
          >
            <el-input
              v-model="branch.expression"
              placeholder="条件表达式，如: field > 10"
              @input="handleConditionChange(index, branch)"
            />
            <el-button type="danger" link @click="handleRemoveBranch(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button type="primary" link @click="handleAddBranch">
            <el-icon><Plus /></el-icon>
            添加条件分支
          </el-button>
        </div>

        <!-- 每个条件分支的映射类型配置 -->
        <div v-for="(branch, index) in formData.conditions" :key="branch.id" class="branch-config">
          <div class="branch-title">分支 {{ index + 1 }}: {{ branch.expression || '未命名' }}</div>
          <MappingTypeConfig
            v-model="formData.conditions[index]"
            :source-data="sourceData"
            :source-path="formData.sourcePath"
          />
        </div>
      </template>

      <!-- 非条件模式：映射类型配置 -->
      <template v-else>
        <el-divider content-position="left">字段映射类型</el-divider>
        <MappingTypeConfig
          v-model="formData"
          :source-data="sourceData"
          :source-path="formData.sourcePath"
        />
      </template>
    </el-form>

    <!-- 预览 -->
    <el-divider content-position="left">字段预览</el-divider>
    <div class="field-preview">
      <div class="preview-label">源字段值:</div>
      <pre class="preview-value">{{ sourceValueStr }}</pre>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" @click="handleDelete">删除</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import type { FieldMapping, ConditionBranch } from '@/types'
import MappingTypeConfig from './MappingTypeConfig.vue'

const props = defineProps<{
  modelValue: boolean
  field: FieldMapping | null
  sourceData: Record<string, unknown> | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', field: FieldMapping): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const formRef = ref()
const formData = reactive<FieldMapping>({
  id: '',
  sourcePath: '',
  targetPath: '',
  targetName: '',
  mappingType: 'direct',
  conditionEnabled: false,
  conditions: [],
  constantValue: undefined,
  functionChain: [],
  enumeration: []
})

// 监听 field 变化
watch(
  () => props.field,
  (newVal) => {
    if (newVal) {
      Object.assign(formData, newVal)
    }
  },
  { immediate: true }
)

// 源字段值
const sourceValueStr = computed(() => {
  if (!props.sourceData || !formData.sourcePath) return '-'
  const value = getNestedValue(props.sourceData, formData.sourcePath)
  return JSON.stringify(value, null, 2)
})

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split('.').reduce((acc: any, key) => acc?.[key], obj)
}

// 条件分支操作
const handleAddBranch = () => {
  const newBranch: ConditionBranch = {
    id: `branch_${Date.now()}`,
    expression: '',
    mappingType: 'direct'
  }
  formData.conditions?.push(newBranch)
}

const handleRemoveBranch = (index: number) => {
  formData.conditions?.splice(index, 1)
}

const handleConditionChange = (index: number, branch: ConditionBranch) => {
  // 可以在这里添加条件表达式验证
}

// 保存
const handleSave = () => {
  emit('save', { ...formData })
  visible.value = false
}

// 删除
const handleDelete = () => {
  emit('save', { ...formData, _delete: true } as any)
  visible.value = false
}
</script>

<style scoped>
.switch-label {
  margin-left: 12px;
  color: var(--text-secondary);
}

.condition-branches {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.condition-branch {
  display: flex;
  align-items: center;
  gap: 8px;
}

.branch-config {
  margin-top: 16px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.branch-title {
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--text-secondary);
}

.field-preview {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 12px;
}

.preview-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 8px;
}

.preview-value {
  font-family: var(--font-mono);
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>