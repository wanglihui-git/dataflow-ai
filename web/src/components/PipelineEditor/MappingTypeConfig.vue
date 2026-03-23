<template>
  <div class="mapping-type-config">
    <!-- 映射类型选择 -->
    <el-form-item label="映射类型">
      <el-radio-group v-model="localMapping.mappingType" @change="handleTypeChange">
        <el-radio-button value="direct">直接引用</el-radio-button>
        <el-radio-button value="constant">常数值</el-radio-button>
        <el-radio-button value="function">函数式</el-radio-button>
        <el-radio-button value="enumerate">枚举</el-radio-button>
      </el-radio-group>
    </el-form-item>

    <!-- Direct 类型 - 无需额外配置 -->
    <template v-if="localMapping.mappingType === 'direct'">
      <el-alert type="info" :closable="false" show-icon>
        直接将源字段值映射到目标字段
      </el-alert>
    </template>

    <!-- Constant 类型 -->
    <template v-else-if="localMapping.mappingType === 'constant'">
      <el-form-item label="常数值">
        <el-input
          v-model="constantValueInput"
          type="textarea"
          :rows="2"
          placeholder="请输入常数值"
          @input="handleConstantChange"
        />
      </el-form-item>
      <el-form-item label="类型推断">
        <el-tag>{{ inferredType }}</el-tag>
      </el-form-item>
    </template>

    <!-- Function 类型 -->
    <template v-else-if="localMapping.mappingType === 'function'">
      <div class="function-chain">
        <div class="function-chain__header">
          <span>函数链（从上往下执行）</span>
          <el-button type="primary" link size="small" @click="handleAddFunction">
            <el-icon><Plus /></el-icon>
            添加函数
          </el-button>
        </div>

        <div class="function-list">
          <div
            v-for="(func, index) in localMapping.functionChain"
            :key="func.id"
            class="function-item"
          >
            <div class="function-item__index">{{ index + 1 }}</div>
            <div class="function-item__content">
              <el-select
                v-model="func.name"
                placeholder="选择函数"
                style="width: 200px"
                @change="handleFunctionChange(index)"
              >
                <el-option-group
                  v-for="group in functionGroups"
                  :key="group.category"
                  :label="group.category"
                >
                  <el-option
                    v-for="f in group.functions"
                    :key="f.name"
                    :label="f.displayName"
                    :value="f.name"
                  />
                </el-option-group>
              </el-select>

              <!-- 函数参数 -->
              <div v-if="func.name" class="function-params">
                <el-form-item
                  v-for="param in getFunctionParams(func.name)"
                  :key="param.name"
                  :label="param.name"
                >
                  <el-input
                    :model-value="func.params[param.name]"
                    :placeholder="param.description"
                    @update:model-value="(val: any) => handleParamChange(index, param.name, val)"
                  />
                </el-form-item>
              </div>
            </div>
            <el-button
              type="danger"
              link
              @click="handleRemoveFunction(index)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 返回值类型转换 -->
      <el-form-item label="返回值类型">
        <el-select v-model="localMapping.functionReturnType" placeholder="自动">
          <el-option label="自动" value="" />
          <el-option label="字符串" value="string" />
          <el-option label="数字" value="number" />
          <el-option label="布尔值" value="boolean" />
          <el-option label="对象" value="object" />
        </el-select>
      </el-form-item>
    </template>

    <!-- Enumerate 类型 -->
    <template v-else-if="localMapping.mappingType === 'enumerate'">
      <div class="enumerate-config">
        <div class="enumerate-header">
          <span>枚举映射</span>
          <el-button type="primary" link size="small" @click="handleAddEnum">
            <el-icon><Plus /></el-icon>
            添加映射
          </el-button>
        </div>
        <el-table :data="localMapping.enumeration" border size="small">
          <el-table-column label="源值" min-width="120">
            <template #default="{ row, $index }">
              <el-input
                v-model="row.value"
                size="small"
                placeholder="源值"
                @input="handleEnumChange($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="目标值" min-width="120">
            <template #default="{ row, $index }">
              <el-input
                v-model="row.label"
                size="small"
                placeholder="目标值"
                @input="handleEnumChange($index)"
              />
            </template>
          </el-table-column>
          <el-table-column width="60">
            <template #default="{ $index }">
              <el-button
                type="danger"
                link
                size="small"
                @click="handleRemoveEnum($index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { FieldMapping, FunctionCall, BuiltinFunction } from '@/types'
import { BUILTIN_FUNCTIONS } from '@/constants/builtinFunctions'

interface Props {
  modelValue: FieldMapping | ConditionBranch
  sourceData: Record<string, unknown> | null
  sourcePath: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: FieldMapping | ConditionBranch): void
}>()

const localMapping = reactive<FieldMapping>({
  id: '',
  sourcePath: '',
  targetPath: '',
  targetName: '',
  mappingType: 'direct',
  conditionEnabled: false,
  constantValue: undefined,
  functionChain: [],
  enumeration: []
})

// 监听 v-model 变化
watch(
  () => props.modelValue,
  (newVal) => {
    Object.assign(localMapping, newVal)
  },
  { immediate: true, deep: true }
)

// 监听内部变化并 emit
watch(
  () => localMapping,
  (newVal) => {
    emit('update:modelValue', { ...newVal })
  },
  { deep: true }
)

// 常数值输入
const constantValueInput = ref('')

watch(
  () => localMapping.constantValue,
  (newVal) => {
    constantValueInput.value = newVal !== undefined ? String(newVal) : ''
  },
  { immediate: true }
)

const inferredType = computed(() => {
  if (!constantValueInput.value) return '-'
  const val = constantValueInput.value
  if (!isNaN(Number(val))) return 'number'
  if (val === 'true' || val === 'false') return 'boolean'
  if (val.startsWith('{') || val.startsWith('[')) return 'object'
  return 'string'
})

const handleConstantChange = () => {
  const val = constantValueInput.value
  if (!val) {
    localMapping.constantValue = undefined
    return
  }
  // 尝试转换类型
  if (!isNaN(Number(val))) {
    localMapping.constantValue = Number(val)
  } else if (val === 'true') {
    localMapping.constantValue = true
  } else if (val === 'false') {
    localMapping.constantValue = false
  } else if (val.startsWith('{') || val.startsWith('[')) {
    try {
      localMapping.constantValue = JSON.parse(val)
    } catch {
      localMapping.constantValue = val
    }
  } else {
    localMapping.constantValue = val
  }
}

const handleTypeChange = () => {
  // 类型变化时的清理工作
}

// 函数相关
const functionGroups = computed(() => {
  const groups: Record<string, BuiltinFunction[]> = {}
  for (const func of BUILTIN_FUNCTIONS) {
    if (!groups[func.category]) {
      groups[func.category] = []
    }
    groups[func.category].push(func)
  }
  return Object.entries(groups).map(([category, functions]) => ({
    category,
    functions
  }))
})

const getFunctionParams = (funcName: string) => {
  const func = BUILTIN_FUNCTIONS.find(f => f.name === funcName)
  return func?.params || []
}

const handleAddFunction = () => {
  const newFunc: FunctionCall = {
    id: `func_${Date.now()}`,
    name: '',
    params: {}
  }
  localMapping.functionChain?.push(newFunc)
}

const handleRemoveFunction = (index: number) => {
  localMapping.functionChain?.splice(index, 1)
}

const handleFunctionChange = (index: number) => {
  const func = localMapping.functionChain?.[index]
  if (func) {
    func.params = {}
  }
}

const handleParamChange = (index: number, paramName: string, value: unknown) => {
  const func = localMapping.functionChain?.[index]
  if (func) {
    func.params[paramName] = value
  }
}

// 枚举相关
const handleAddEnum = () => {
  localMapping.enumeration?.push({ value: '', label: '' })
}

const handleRemoveEnum = (index: number) => {
  localMapping.enumeration?.splice(index, 1)
}

const handleEnumChange = () => {
  // 枚举变化处理
}
</script>

<style scoped>
.mapping-type-config {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.function-chain {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 16px;
}

.function-chain__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
}

.function-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.function-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--bg-primary);
  border-radius: var(--radius-md);
}

.function-item__index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 500;
}

.function-item__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.function-params {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.enumerate-config {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: 16px;
}

.enumerate-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
}
</style>