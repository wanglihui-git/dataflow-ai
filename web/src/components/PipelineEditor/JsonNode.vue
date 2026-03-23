<template>
  <div class="json-node" :style="{ paddingLeft: depth * 16 + 'px' }">
    <div
      class="json-node__row"
      :draggable="draggable"
      @click="handleClick"
      @dragstart="handleDragStart"
    >
      <!-- 展开/折叠按钮 -->
      <span
        v-if="hasChildren"
        class="json-node__toggle"
        @click.stop="toggle"
      >
        <el-icon>
          <ArrowRight v-if="!expanded" />
          <ArrowDown v-else />
        </el-icon>
      </span>
      <span v-else class="json-node__toggle-placeholder"></span>

      <!-- 键 -->
      <span class="json-node__key">{{ node.key }}</span>
      <span class="json-node__colon">:</span>

      <!-- 值 -->
      <span :class="['json-node__value', `json-node__value--${node.type}`]">
        <template v-if="node.type === 'object'">
          {{ expanded ? '{' : '{...}' }}
        </template>
        <template v-else-if="node.type === 'array'">
          {{ expanded ? '[' : `[${node.children?.length || 0}]` }}
        </template>
        <template v-else-if="node.type === 'string'">
          "{{ node.value }}"
        </template>
        <template v-else-if="node.value === null">
          null
        </template>
        <template v-else>
          {{ node.value }}
        </template>
      </span>
    </div>

    <!-- 子节点 -->
    <div v-if="hasChildren && expanded" class="json-node__children">
      <JsonNode
        v-for="child in node.children"
        :key="child.path"
        :node="child"
        :depth="depth + 1"
        :mode="mode"
        :draggable="draggable"
        @field-click="(field) => emit('fieldClick', field)"
        @field-dragstart="(field) => emit('fieldDragstart', field)"
      />
      <div v-if="node.type === 'object' && mode === 'target'" class="json-node__add">
        <el-button type="primary" link size="small" @click.stop="handleAddChild">
          <el-icon><Plus /></el-icon>
          添加字段
        </el-button>
      </div>
    </div>

    <!-- 结束括号 -->
    <div v-if="hasChildren && expanded && node.type === 'object'" class="json-node__end">
      }
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ArrowRight, ArrowDown, Plus } from '@element-plus/icons-vue'

interface JsonNodeData {
  path: string
  key: string
  value?: unknown
  type: string
  expanded?: boolean
  children?: JsonNodeData[]
}

interface Props {
  node: JsonNodeData
  depth: number
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
}>()

const expanded = ref(props.node.expanded ?? true)

const hasChildren = computed(() => {
  return props.node.children && props.node.children.length > 0
})

const toggle = () => {
  expanded.value = !expanded.value
}

const handleClick = () => {
  if (!hasChildren.value) {
    emit('fieldClick', { path: props.node.path, value: props.node.value })
  }
}

const handleDragStart = (e: DragEvent) => {
  if (props.draggable && !hasChildren.value) {
    e.dataTransfer?.setData('application/json', JSON.stringify({
      path: props.node.path,
      value: props.node.value
    }))
    emit('fieldDragstart', { path: props.node.path, value: props.node.value })
  }
}

const handleAddChild = () => {
  // TODO: 实现添加子节点功能
}
</script>

<style scoped>
.json-node {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.json-node__row {
  display: flex;
  align-items: center;
  padding: 2px 0;
  border-radius: 4px;
  cursor: pointer;
}

.json-node__row:hover {
  background: var(--bg-secondary);
}

.json-node__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  cursor: pointer;
  color: var(--text-tertiary);
}

.json-node__toggle-placeholder {
  width: 16px;
}

.json-node__key {
  color: var(--color-primary);
  margin-right: 4px;
}

.json-node__colon {
  color: var(--text-secondary);
  margin-right: 8px;
}

.json-node__value {
  word-break: break-all;
}

.json-node__value--string {
  color: var(--color-success);
}

.json-node__value--number {
  color: var(--color-danger);
}

.json-node__value--boolean {
  color: var(--color-warning);
}

.json-node__value--null {
  color: var(--text-tertiary);
  font-style: italic;
}

.json-node__value--object,
.json-node__value--array {
  color: var(--text-secondary);
}

.json-node__children {
  padding-left: 8px;
}

.json-node__end {
  padding-left: 24px;
  color: var(--text-secondary);
}

.json-node__add {
  padding: 4px 0 4px 24px;
}
</style>