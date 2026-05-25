<template>
  <div class="pipeline-editor">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="emit('back')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
        <el-input v-model="localName" placeholder="Pipeline 名称" class="name-input" />
      </div>
      <div class="toolbar-right">
        <el-button @click="undo" :disabled="!canUndo">撤销</el-button>
        <el-button @click="redo" :disabled="!canRedo">重做</el-button>
        <el-button @click="validate">校验</el-button>
        <el-button @click="handlePreview">预览</el-button>
        <el-button @click="showAi = true"><el-icon><MagicStick /></el-icon>AI</el-button>
        <el-button @click="exportJson">导出</el-button>
        <el-button @click="importJson">导入</el-button>
        <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="save(true)">保存并发布</el-button>
        <el-button type="success" :loading="running" @click="emit('run')">运行</el-button>
      </div>
    </div>

    <div class="editor-body">
      <aside class="palette">
        <h4>组件库</h4>
        <el-collapse v-model="openGroups">
          <el-collapse-item title="输入" name="in">
            <div class="palette-item" @click="selectNode('source')">Source 源</div>
          </el-collapse-item>
          <el-collapse-item title="转换" name="tx">
            <div
              v-for="t in TRANSFORM_TYPES"
              :key="t.type"
              class="palette-item"
              draggable="true"
              @dragstart="onDragStart($event, t.type)"
              @click="addTransform(t.type)"
            >
              {{ t.label }}
            </div>
          </el-collapse-item>
          <el-collapse-item title="输出" name="out">
            <div class="palette-item" @click="selectNode('sink')">Sink 目标</div>
          </el-collapse-item>
          <el-collapse-item title="规划" name="plan">
            <div class="palette-item muted">条件 / 分支（二期）</div>
          </el-collapse-item>
        </el-collapse>
      </aside>

      <div class="canvas-wrap" @drop="onDrop" @dragover.prevent>
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          :fit-view-on-init="true"
          @node-click="onNodeClick"
        >
          <Background pattern-color="#e2e8f0" :gap="16" />
          <MiniMap />
          <Controls />
        </VueFlow>
      </div>

      <aside class="props-panel">
        <h4>属性</h4>
        <template v-if="selectedId === 'source'">
          <el-form label-position="top" size="small">
            <el-form-item label="数据源">
              <el-select v-model="source.dataSourceId" filterable placeholder="选择数据源" @change="syncUp">
                <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="表名">
              <el-input v-model="source.tableName" @change="syncUp" />
            </el-form-item>
            <el-form-item label="查询 SQL">
              <el-input v-model="source.query" type="textarea" @change="syncUp" />
            </el-form-item>
          </el-form>
        </template>
        <template v-else-if="selectedId === 'sink'">
          <el-form label-position="top" size="small">
            <el-form-item label="目标数据源">
              <el-select v-model="sink.dataSourceId" filterable @change="syncUp">
                <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="表名">
              <el-input v-model="sink.tableName" @change="syncUp" />
            </el-form-item>
            <el-form-item label="写入模式">
              <el-select v-model="sink.writeMode" @change="syncUp">
                <el-option label="APPEND" value="APPEND" />
                <el-option label="OVERWRITE" value="OVERWRITE" />
              </el-select>
            </el-form-item>
          </el-form>
        </template>
        <TransformConfigForm
          v-else-if="selectedTransform"
          :transform="selectedTransform"
          @update="updateTransform"
        />
        <EmptyState v-else title="选择节点" description="点击画布上的节点编辑属性" />
      </aside>
    </div>

    <div class="editor-footer">
      <el-input v-model="changeNote" placeholder="本地变更说明（不入库）" />
      <span v-if="savedUpdatedAt" class="conflict-hint">上次保存：{{ savedUpdatedAt }}</span>
    </div>

    <AiDrawer v-model="showAi" :pipeline-id="pipelineId" @apply="applyAiNodes" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/minimap/dist/style.css'
import '@vue-flow/controls/dist/style.css'
import { ElMessage } from 'element-plus'
import { ArrowLeft, MagicStick } from '@element-plus/icons-vue'
import type { Node, Edge } from '@vue-flow/core'
import type { DataSource, SourceConfig, SinkConfig, Transform, TransformType } from '@/types'
import {
  TRANSFORM_TYPES,
  newNodeId,
  topologicalSort,
  hasCycle,
  defaultConfigForType,
  buildDependsOnChain
} from '@/utils/pipelineGraph'
import TransformConfigForm from './TransformConfigForm.vue'
import AiDrawer from './AiDrawer.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const props = defineProps<{
  pipelineId?: string
  name: string
  source: SourceConfig
  sink: SinkConfig
  transforms: Transform[]
  dataSources: DataSource[]
  saving?: boolean
  running?: boolean
  savedUpdatedAt?: string
}>()

const emit = defineEmits<{
  (e: 'update:name', v: string): void
  (e: 'update:source', v: SourceConfig): void
  (e: 'update:sink', v: SinkConfig): void
  (e: 'update:transforms', v: Transform[]): void
  (e: 'save', publish: boolean): void
  (e: 'run'): void
  (e: 'preview'): void
  (e: 'back'): void
}>()

const localName = ref(props.name)
const source = ref<SourceConfig>({ ...props.source })
const sink = ref<SinkConfig>({ ...props.sink })
const transforms = ref<Transform[]>([...(props.transforms || [])])
const selectedId = ref<string>('source')
const showAi = ref(false)
const changeNote = ref('')
const openGroups = ref(['in', 'tx', 'out'])

const history = ref<string[]>([])
const historyIndex = ref(-1)

watch(
  () => props.name,
  (v) => (localName.value = v)
)
watch(
  () => props.source,
  (v) => {
    source.value = { ...v }
  },
  { deep: true }
)
watch(
  () => props.sink,
  (v) => {
    sink.value = { ...v }
  },
  { deep: true }
)
watch(
  () => props.transforms,
  (v) => {
    transforms.value = [...(v || [])]
    rebuildGraph()
  },
  { deep: true }
)
watch(localName, (v) => emit('update:name', v))

function snapshot() {
  const state = JSON.stringify({ source: source.value, sink: sink.value, transforms: transforms.value })
  history.value = history.value.slice(0, historyIndex.value + 1)
  history.value.push(state)
  historyIndex.value = history.value.length - 1
  rebuildGraph()
}

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

function undo() {
  if (!canUndo.value) return
  historyIndex.value--
  applyHistory(history.value[historyIndex.value])
}
function redo() {
  if (!canRedo.value) return
  historyIndex.value++
  applyHistory(history.value[historyIndex.value])
}
function applyHistory(json: string) {
  const s = JSON.parse(json)
  source.value = s.source
  sink.value = s.sink
  transforms.value = s.transforms
  syncUp()
  rebuildGraph()
}

const nodes = ref<Node[]>([])
const edges = ref<Edge[]>([])

function rebuildGraph() {
  const sorted = topologicalSort(transforms.value)
  const list: Node[] = [
    {
      id: 'source',
      type: 'default',
      position: { x: 80, y: 40 },
      label: 'Source',
      data: { kind: 'source' }
    }
  ]
  sorted.forEach((t, i) => {
    list.push({
      id: t.nodeId,
      type: 'default',
      position: { x: 80, y: 120 + i * 90 },
      label: t.name || t.type,
      data: { kind: 'transform', type: t.type }
    })
  })
  list.push({
    id: 'sink',
    type: 'default',
    position: { x: 80, y: 120 + sorted.length * 90 },
    label: 'Sink',
    data: { kind: 'sink' }
  })
  nodes.value = list

  const es: Edge[] = []
  if (sorted.length) {
    es.push({ id: 'e-s-t0', source: 'source', target: sorted[0].nodeId })
    for (let i = 1; i < sorted.length; i++) {
      es.push({ id: `e-${i}`, source: sorted[i - 1].nodeId, target: sorted[i].nodeId })
    }
    es.push({ id: 'e-t-sink', source: sorted[sorted.length - 1].nodeId, target: 'sink' })
  } else {
    es.push({ id: 'e-direct', source: 'source', target: 'sink' })
  }
  edges.value = es
}

function selectNode(id: string) {
  selectedId.value = id
}

function onNodeClick({ node }: { node: Node }) {
  selectedId.value = node.id
}

const selectedTransform = computed(() =>
  transforms.value.find((t) => t.nodeId === selectedId.value)
)

function addTransform(type: TransformType) {
  const t: Transform = {
    nodeId: newNodeId(),
    type,
    name: TRANSFORM_TYPES.find((x) => x.type === type)?.label || type,
    config: defaultConfigForType(type),
    dependsOn: []
  }
  transforms.value = buildDependsOnChain([...transforms.value, t])
  snapshot()
  selectedId.value = t.nodeId
}

function onDragStart(ev: DragEvent, type: TransformType) {
  ev.dataTransfer?.setData('transformType', type)
}

function onDrop(ev: DragEvent) {
  const type = ev.dataTransfer?.getData('transformType') as TransformType
  if (type) addTransform(type)
}

function updateTransform(t: Transform) {
  transforms.value = transforms.value.map((x) => (x.nodeId === t.nodeId ? t : x))
  snapshot()
}

function syncUp() {
  emit('update:source', { ...source.value })
  emit('update:sink', { ...sink.value })
  emit('update:transforms', [...transforms.value])
}

function validate() {
  if (!source.value.dataSourceId) {
    ElMessage.warning('请配置 Source 数据源')
    return false
  }
  if (!sink.value.dataSourceId) {
    ElMessage.warning('请配置 Sink 数据源')
    return false
  }
  if (hasCycle(transforms.value)) {
    ElMessage.error('Transform 依赖存在环')
    return false
  }
  ElMessage.success('校验通过')
  return true
}

function save(publish: boolean) {
  if (!validate()) return
  emit('save', publish)
}

function handlePreview() {
  emit('preview')
}

function exportJson() {
  const blob = new Blob(
    [
      JSON.stringify(
        { name: localName.value, source: source.value, transforms: transforms.value, sink: sink.value },
        null,
        2
      )
    ],
    { type: 'application/json' }
  )
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${localName.value || 'pipeline'}.json`
  a.click()
}

function importJson() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    const text = await file.text()
    const data = JSON.parse(text)
    if (data.name) localName.value = data.name
    if (data.source) source.value = data.source
    if (data.sink) sink.value = data.sink
    if (data.transforms) transforms.value = data.transforms
    snapshot()
    syncUp()
  }
  input.click()
}

function applyAiNodes(nodes: Record<string, unknown>[]) {
  const mapped: Transform[] = nodes.map((n) => ({
    nodeId: (n.nodeId as string) || newNodeId(),
    type: (n.type as TransformType) || 'FIELD_MAPPER',
    name: (n.name as string) || 'AI 节点',
    config: (n.config as Record<string, unknown>) || {},
    dependsOn: (n.dependsOn as string[]) || [],
    generatedBy: (n.generatedBy as string) || undefined
  }))
  transforms.value = buildDependsOnChain(mapped)
  snapshot()
  showAi.value = false
}

snapshot()
</script>

<style scoped>
.pipeline-editor {
  height: calc(100vh - 0px);
  display: flex;
  flex-direction: column;
  background: #fff;
}
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.name-input {
  width: 220px;
}
.editor-body {
  flex: 1;
  display: grid;
  grid-template-columns: 200px 1fr 280px;
  min-height: 0;
}
.palette,
.props-panel {
  border-right: 1px solid #e2e8f0;
  padding: 12px;
  overflow: auto;
}
.props-panel {
  border-right: none;
  border-left: 1px solid #e2e8f0;
}
.palette h4,
.props-panel h4 {
  margin: 0 0 12px;
  font-size: 14px;
}
.palette-item {
  padding: 8px 10px;
  margin-bottom: 4px;
  background: #f8fafc;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.palette-item:hover {
  background: #e0f2fe;
}
.palette-item.muted {
  color: #94a3b8;
  cursor: default;
}
.canvas-wrap {
  height: 100%;
  min-height: 400px;
}
.editor-footer {
  padding: 8px 12px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  gap: 12px;
  align-items: center;
}
.conflict-hint {
  font-size: 12px;
  color: #64748b;
}
</style>
