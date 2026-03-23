<template>
  <div class="json-editor" :class="{ 'json-editor--draggable': draggable && mode === 'source' }">
    <JsonNode
      v-for="node in nodes"
      :key="node.path"
      :node="node"
      :depth="0"
      :mode="mode"
      :draggable="draggable && mode === 'source'"
      @field-click="(field) => emit('fieldClick', field)"
      @field-dragstart="(field) => emit('fieldDragstart', field)"
      @node-add="(path, field) => emit('nodeAdd', path, field)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { JsonObject } from '@/types'
import JsonNode from './JsonNode.vue'

interface Props {
  data: Record<string, unknown> | JsonObject
  mode?: 'source' | 'target'
  draggable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'source',
  draggable: false
})

const emit = defineEmits<{
  (e: 'fieldClick', field: { path: string; value: unknown }): void
  (e: 'fieldDragstart', field: { path: string; value: unknown }): void
  (e: 'nodeAdd', parentPath: string, field: JsonObject): void
}>()

const nodes = computed(() => {
  if (props.mode === 'target' && 'type' in props.data) {
    // 目标模式：已经是 JsonObject 结构
    return props.data.children || []
  }
  // 源数据模式：转换为节点数组
  return convertToNodes(props.data as Record<string, unknown>, '')
})

function convertToNodes(
  obj: Record<string, unknown>,
  parentPath: string
): Array<{ path: string; key: string; value: unknown; type: string; children?: Array<any> }> {
  const nodes: Array<any> = []

  for (const [key, value] of Object.entries(obj)) {
    const path = parentPath ? `${parentPath}.${key}` : key
    const type = Array.isArray(value) ? 'array' : typeof value

    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      nodes.push({
        path,
        key,
        value: null,
        type: 'object',
        expanded: true,
        children: convertToNodes(value as Record<string, unknown>, path)
      })
    } else if (Array.isArray(value)) {
      nodes.push({
        path,
        key,
        value: `[${value.length} items]`,
        type: 'array',
        expanded: false,
        children: value.map((item, index) => ({
          path: `${path}[${index}]`,
          key: String(index),
          value: item,
          type: typeof item
        }))
      })
    } else {
      nodes.push({
        path,
        key,
        value,
        type
      })
    }
  }

  return nodes
}
</script>

<style scoped>
.json-editor {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.json-editor--draggable {
  cursor: grab;
}

.json-editor--draggable:active {
  cursor: grabbing;
}
</style>