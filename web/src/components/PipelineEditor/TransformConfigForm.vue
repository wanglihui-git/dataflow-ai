<template>
  <el-form label-position="top" size="small">
    <el-form-item label="节点名称">
      <el-input :model-value="transform.name" @update:model-value="emitName" />
    </el-form-item>
    <template v-if="transform.type === 'FIELD_MAPPER'">
      <el-form-item label="字段映射 (JSON)">
        <el-input v-model="configJson" type="textarea" :rows="6" @blur="syncConfig" />
      </el-form-item>
    </template>
    <template v-else-if="transform.type === 'FILTER'">
      <el-form-item label="字段">
        <el-input v-model="localConfig.field" @change="patch" />
      </el-form-item>
      <el-form-item label="运算符">
        <el-select v-model="localConfig.operator" @change="patch">
          <el-option label="等于" value="eq" />
          <el-option label="大于" value="gt" />
          <el-option label="包含" value="contains" />
        </el-select>
      </el-form-item>
      <el-form-item label="值">
        <el-input v-model="localConfig.value" @change="patch" />
      </el-form-item>
    </template>
    <template v-else-if="transform.type === 'SCRIPT'">
      <el-form-item label="脚本">
        <el-input v-model="localConfig.script" type="textarea" :rows="8" @change="patch" />
      </el-form-item>
      <el-form-item label="输出字段">
        <el-input v-model="localConfig.outputField" @change="patch" />
      </el-form-item>
    </template>
    <template v-else-if="transform.type === 'AI_ASSISTED'">
      <el-form-item label="Prompt">
        <el-input v-model="localConfig.prompt" type="textarea" :rows="4" @change="patch" />
      </el-form-item>
      <el-form-item label="输出字段">
        <el-input v-model="localConfig.outputField" @change="patch" />
      </el-form-item>
    </template>
    <template v-else-if="transform.type === 'LOOKUP'">
      <el-form-item label="数据源 ID">
        <el-input v-model="localConfig.dataSourceId" @change="patch" />
      </el-form-item>
      <el-form-item label="查找表">
        <el-input v-model="localConfig.lookupTable" @change="patch" />
      </el-form-item>
      <el-form-item label="查找键">
        <el-input v-model="localConfig.lookupKey" @change="patch" />
      </el-form-item>
    </template>
    <template v-else>
      <el-form-item label="配置 (JSON)">
        <el-input v-model="configJson" type="textarea" :rows="8" @blur="syncConfig" />
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Transform } from '@/types'

const props = defineProps<{ transform: Transform }>()
const emit = defineEmits<{ (e: 'update', t: Transform): void }>()

const localConfig = ref<Record<string, unknown>>({ ...(props.transform.config || {}) })
const configJson = ref(JSON.stringify(props.transform.config || {}, null, 2))

watch(
  () => props.transform.nodeId,
  () => {
    localConfig.value = { ...(props.transform.config || {}) }
    configJson.value = JSON.stringify(props.transform.config || {}, null, 2)
  }
)

function patch() {
  emit('update', { ...props.transform, config: { ...localConfig.value } })
}

function syncConfig() {
  try {
    const parsed = JSON.parse(configJson.value)
    emit('update', { ...props.transform, config: parsed })
  } catch {
    /* invalid json */
  }
}

function emitName(v: string) {
  emit('update', { ...props.transform, name: v })
}
</script>
