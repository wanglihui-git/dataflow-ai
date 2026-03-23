import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Pipeline, DataSource, FieldMapping, JsonObject, DraggingField } from '@/types'
import { MappingType } from '@/types'

export const useEditorStore = defineStore('editor', () => {
  // 当前编辑的 Pipeline
  const currentPipeline = ref<Pipeline | null>(null)

  // 选中的数据源
  const selectedDataSource = ref<DataSource | null>(null)

  // 源数据预览
  const sourceDataPreview = ref<Record<string, unknown> | null>(null)

  // 目标 JSON 结构
  const targetSchema = ref<JsonObject>({
    type: 'object',
    name: 'root',
    path: '',
    children: []
  })

  // 字段映射配置
  const fieldMappings = ref<FieldMapping[]>([])

  // 是否已修改
  const isDirty = ref(false)

  // 当前拖拽的字段
  const draggingField = ref<DraggingField | null>(null)

  // 预览数据
  const previewData = ref<Record<string, unknown> | null>(null)

  // 加载状态
  const loading = ref(false)

  // 当前选中的字段（用于编辑）
  const selectedField = ref<FieldMapping | null>(null)

  // 是否显示预览弹窗
  const showPreviewDialog = ref(false)

  // 计算属性
  const hasChanges = computed(() => isDirty.value)
  const sourceFields = computed(() => {
    if (!sourceDataPreview.value) return []
    return extractFields(sourceDataPreview.value, '')
  })

  // 辅助函数：提取字段路径
  function extractFields(obj: Record<string, unknown>, prefix: string): Array<{ path: string; value: unknown; type: string }> {
    const fields: Array<{ path: string; value: unknown; type: string }> = []

    for (const [key, value] of Object.entries(obj)) {
      const currentPath = prefix ? `${prefix}.${key}` : key
      const type = Array.isArray(value) ? 'array' : typeof value

      if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
        fields.push(...extractFields(value as Record<string, unknown>, currentPath))
      } else {
        fields.push({
          path: currentPath,
          value,
          type
        })
      }
    }

    return fields
  }

  // 初始化编辑器
  function initEditor(pipeline?: Pipeline) {
    if (pipeline) {
      currentPipeline.value = pipeline
      // 从 pipeline 解析 fieldMappings
      if (pipeline.transforms?.length) {
        // TODO: 从 transforms 解析 fieldMappings
      }
    } else {
      currentPipeline.value = null
    }
    resetEditor()
  }

  // 重置编辑器状态
  function resetEditor() {
    selectedDataSource.value = null
    sourceDataPreview.value = null
    targetSchema.value = {
      type: 'object',
      name: 'root',
      path: '',
      children: []
    }
    fieldMappings.value = []
    isDirty.value = false
    draggingField.value = null
    previewData.value = null
    selectedField.value = null
    showPreviewDialog.value = false
  }

  // 设置数据源
  function setDataSource(dataSource: DataSource, preview: Record<string, unknown>) {
    selectedDataSource.value = dataSource
    sourceDataPreview.value = preview
    isDirty.value = true
  }

  // 开始拖拽
  function startDrag(field: DraggingField) {
    draggingField.value = field
  }

  // 结束拖拽
  function endDrag() {
    draggingField.value = null
  }

  // 添加字段映射
  function addFieldMapping(sourcePath: string, targetPath: string, targetName: string) {
    const mapping: FieldMapping = {
      id: `mapping_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      sourcePath,
      targetPath,
      targetName,
      mappingType: MappingType.DIRECT,
      conditionEnabled: false
    }
    fieldMappings.value.push(mapping)
    isDirty.value = true
    return mapping
  }

  // 更新字段映射
  function updateFieldMapping(id: string, updates: Partial<FieldMapping>) {
    const index = fieldMappings.value.findIndex(m => m.id === id)
    if (index !== -1) {
      fieldMappings.value[index] = { ...fieldMappings.value[index], ...updates }
      isDirty.value = true
    }
  }

  // 删除字段映射
  function removeFieldMapping(id: string) {
    fieldMappings.value = fieldMappings.value.filter(m => m.id !== id)
    isDirty.value = true
  }

  // 选择字段进行编辑
  function selectField(field: FieldMapping | null) {
    selectedField.value = field
  }

  // 设置目标 JSON 结构
  function setTargetSchema(schema: JsonObject) {
    targetSchema.value = schema
    isDirty.value = true
  }

  // 添加目标字段
  function addTargetField(parentPath: string, field: JsonObject) {
    const parent = findNode(targetSchema.value, parentPath)
    if (parent && parent.children) {
      parent.children.push(field)
      isDirty.value = true
    }
  }

  // 查找节点
  function findNode(node: JsonObject, path: string): JsonObject | null {
    if (node.path === path) return node
    if (node.children) {
      for (const child of node.children) {
        const found = findNode(child, path)
        if (found) return found
      }
    }
    return null
  }

  // 标记为已保存
  function markAsSaved() {
    isDirty.value = false
  }

  // 显示预览
  function openPreview() {
    showPreviewDialog.value = true
  }

  // 隐藏预览
  function closePreview() {
    showPreviewDialog.value = false
  }

  // 设置预览数据
  function setPreviewData(data: Record<string, unknown>) {
    previewData.value = data
  }

  return {
    currentPipeline,
    selectedDataSource,
    sourceDataPreview,
    targetSchema,
    fieldMappings,
    isDirty,
    draggingField,
    previewData,
    loading,
    selectedField,
    showPreviewDialog,
    hasChanges,
    sourceFields,
    initEditor,
    resetEditor,
    setDataSource,
    startDrag,
    endDrag,
    addFieldMapping,
    updateFieldMapping,
    removeFieldMapping,
    selectField,
    setTargetSchema,
    addTargetField,
    markAsSaved,
    openPreview,
    closePreview,
    setPreviewData
  }
})