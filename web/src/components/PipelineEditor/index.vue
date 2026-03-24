<template>
  <div class="pipeline-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-input
          v-model="pipelineName"
          placeholder="Pipeline 名称"
          class="pipeline-name-input"
          @change="handleNameChange"
        />
      </div>
      <div class="toolbar-right">
        <el-button @click="handlePreview">
          <el-icon><View /></el-icon>
          预览
        </el-button>
        <el-button @click="handleAIPanel">
          <el-icon><MagicStick /></el-icon>
          AI 辅助
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="editor-content">
      <!-- 左侧：源数据面板 -->
      <div class="source-panel">
        <div class="panel-header">
          <span>源数据</span>
          <el-button type="primary" link size="small" @click="handleSelectDataSource">
            选择数据源
          </el-button>
        </div>
        <div class="panel-content">
          <JsonEditor
            v-if="sourceData"
            :data="sourceData"
            :draggable="true"
            mode="source"
            @field-click="handleSourceFieldClick"
            @field-dragstart="handleFieldDragStart"
          />
          <EmptyState
            v-else
            type="data"
            title="请选择数据源"
            description="点击上方选择数据源按钮来配置源数据"
          >
            <template #actions>
              <el-button type="primary" @click="handleSelectDataSource">
                选择数据源
              </el-button>
            </template>
          </EmptyState>
        </div>
      </div>

      <!-- 分隔线 -->
      <div class="editor-divider">
        <el-icon><Right /></el-icon>
      </div>

      <!-- 右侧：输出配置面板 -->
      <div class="output-panel">
        <div class="panel-header">
          <span>输出配置</span>
          <div class="panel-actions">
            <el-button type="primary" link size="small" @click="handleAddObject">
              <el-icon><Plus /></el-icon>
              添加对象
            </el-button>
          </div>
        </div>
        <div
          class="panel-content drop-zone"
          :class="{ 'drag-over': isDragOver }"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @drop="handleDrop"
        >
          <JsonEditor
            :data="targetSchema"
            mode="target"
            @field-click="handleTargetFieldClick"
            @node-add="handleNodeAdd"
          />
        </div>
      </div>
    </div>

    <!-- 数据源选择对话框 -->
    <DataSourceSelector
      v-model="dataSourceSelectorVisible"
      @select="handleDataSourceSelect"
    />

    <!-- 字段编辑对话框 -->
    <FieldEditor
      v-model="fieldEditorVisible"
      :field="editingField"
      :source-data="sourceData"
      @save="handleFieldSave"
    />

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="数据预览"
      width="800px"
    >
      <pre class="preview-json">{{ previewJson }}</pre>
      <template #footer>
        <el-button @click="previewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, View, MagicStick, Check, Right, Plus } from '@element-plus/icons-vue'
import type { DataSource, FieldMapping, JsonObject } from '@/types'
import { useEditorStore } from '@/stores/editor'
import { usePipelineStore } from '@/stores/pipeline'
import JsonEditor from './JsonEditor.vue'
import DataSourceSelector from './DataSourceSelector.vue'
import FieldEditor from './FieldEditor.vue'
import EmptyState from '@/components/Common/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const editorStore = useEditorStore()
const pipelineStore = usePipelineStore()

const pipelineId = computed(() => route.params.id as string)
const pipelineName = ref('')
const saving = ref(false)

// 数据源选择
const dataSourceSelectorVisible = ref(false)
const sourceData = ref<Record<string, unknown> | null>(null)

// 拖拽状态
const isDragOver = ref(false)

// 字段编辑
const fieldEditorVisible = ref(false)
const editingField = ref<FieldMapping | null>(null)

// 预览
const previewDialogVisible = ref(false)
const previewJson = ref('')

// 目标 Schema
const targetSchema = computed(() => editorStore.targetSchema)

// 初始化
const initEditor = async () => {
  if (pipelineId.value) {
    const pipeline = await pipelineStore.fetchPipelineById(pipelineId.value)
    if (pipeline) {
      pipelineName.value = pipeline.name
      editorStore.initEditor(pipeline)
    }
  } else {
    editorStore.initEditor()
  }
}

// 选择数据源
const handleSelectDataSource = () => {
  dataSourceSelectorVisible.value = true
}

const handleDataSourceSelect = async (dataSource: DataSource, preview: Record<string, unknown>) => {
  sourceData.value = preview
  editorStore.setDataSource(dataSource, preview)
  dataSourceSelectorVisible.value = false
}

// 拖拽处理
const handleFieldDragStart = (field: { path: string; value: unknown }) => {
  editorStore.startDrag({
    sourcePath: field.path,
    sourceValue: field.value,
    sourceType: typeof field.value
  })
}

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = true
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = false

  if (editorStore.draggingField) {
    const mapping = editorStore.addFieldMapping(
      editorStore.draggingField.sourcePath,
      '',
      editorStore.draggingField.sourcePath.split('.').pop() || ''
    )
    // 打开字段编辑
    editingField.value = mapping
    fieldEditorVisible.value = true
  }

  editorStore.endDrag()
}

// 字段点击
const handleSourceFieldClick = (field: { path: string; value: unknown }) => {
  // 可以显示字段详情
  console.log('Source field clicked:', field)
}

const handleTargetFieldClick = (field: JsonObject) => {
  // 查找对应的映射
  const mapping = editorStore.fieldMappings.find(m => m.targetPath === field.path)
  if (mapping) {
    editingField.value = mapping
    fieldEditorVisible.value = true
  }
}

// 添加节点
const handleAddObject = () => {
  const newField: JsonObject = {
    type: 'object',
    name: 'newField',
    path: `field_${Date.now()}`,
    children: []
  }
  editorStore.addTargetField('', newField)
}

const handleNodeAdd = (parentPath: string, field: JsonObject) => {
  editorStore.addTargetField(parentPath, field)
}

// 字段保存
const handleFieldSave = (mapping: FieldMapping) => {
  if (editingField.value) {
    editorStore.updateFieldMapping(mapping.id, mapping)
  }
  fieldEditorVisible.value = false
}

// 预览
const handlePreview = async () => {
  previewJson.value = JSON.stringify(editorStore.fieldMappings, null, 2)
  previewDialogVisible.value = true
}

// AI 面板
const handleAIPanel = () => {
  // 打开 AI 辅助面板
  // TODO: 实现 AI 辅助功能
}

// 保存
const handleSave = async () => {
  if (!pipelineName.value) {
    ElMessage.warning('请输入 Pipeline 名称')
    return
  }

  saving.value = true
  try {
    const data = {
      name: pipelineName.value,
      transforms: editorStore.fieldMappings.map(m => ({
        id: m.id,
        type: 'FIELD_MAPPER',
        name: m.targetName,
        config: {
          sourcePath: m.sourcePath,
          targetPath: m.targetPath,
          targetName: m.targetName,
          mappingType: m.mappingType,
          conditionEnabled: m.conditionEnabled,
          conditions: m.conditions,
          constantValue: m.constantValue,
          functionChain: m.functionChain,
          enumeration: m.enumeration
        }
      }))
    }

    if (pipelineId.value) {
      await pipelineStore.updatePipeline(pipelineId.value, data)
    } else {
      await pipelineStore.createPipeline(data)
    }

    ElMessage.success('保存成功')
    editorStore.markAsSaved()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 名称变更
const handleNameChange = () => {
  // 名称变更处理
}

// 返回
const handleBack = () => {
  if (editorStore.hasChanges) {
    // 提示保存
  }
  router.back()
}

// 初始化
initEditor()
</script>

<style scoped>
.pipeline-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-secondary);
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pipeline-name-input {
  width: 300px;
}

.editor-content {
  display: flex;
  flex: 1;
  padding: 20px;
  gap: 20px;
  overflow: hidden;
}

.source-panel,
.output-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color-light);
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color-light);
  font-weight: 600;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow: auto;
}

.editor-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.drop-zone {
  min-height: 200px;
  transition: all var(--transition-base);
}

.drop-zone.drag-over {
  border-color: var(--color-primary);
  background-color: var(--color-primary-light);
}

.preview-json {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
  font-family: var(--font-mono);
  font-size: 12px;
  max-height: 500px;
  overflow: auto;
  margin: 0;
}
</style>