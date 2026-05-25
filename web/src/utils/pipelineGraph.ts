import type { Transform, TransformType } from '@/types'

export const TRANSFORM_TYPES: { type: TransformType; label: string; group: string }[] = [
  { type: 'FIELD_MAPPER', label: '字段映射', group: '转换' },
  { type: 'FILTER', label: '过滤', group: '转换' },
  { type: 'FLATTEN', label: '扁平化', group: '转换' },
  { type: 'LOOKUP', label: '查找', group: '转换' },
  { type: 'SCRIPT', label: '脚本', group: '转换' },
  { type: 'AI_ASSISTED', label: 'AI 辅助', group: '转换' },
  { type: 'AGGREGATE', label: '聚合', group: '转换' },
  { type: 'JOIN', label: '连接', group: '转换' },
  { type: 'SORT', label: '排序', group: '转换' },
  { type: 'GROUP', label: '分组', group: '转换' }
]

export function newNodeId(): string {
  return `t_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 6)}`
}

/** 拓扑排序 transforms，用于画布纵向布局 */
export function topologicalSort(transforms: Transform[]): Transform[] {
  const map = new Map(transforms.map((t) => [t.nodeId, t]))
  const visited = new Set<string>()
  const result: Transform[] = []

  function visit(id: string) {
    if (visited.has(id)) return
    const node = map.get(id)
    if (!node) return
    for (const dep of node.dependsOn ?? []) {
      visit(dep)
    }
    visited.add(id)
    result.push(node)
  }

  for (const t of transforms) visit(t.nodeId)
  return result
}

export function hasCycle(transforms: Transform[]): boolean {
  const graph = new Map<string, string[]>()
  for (const t of transforms) {
    graph.set(t.nodeId, t.dependsOn ?? [])
  }
  const visiting = new Set<string>()
  const done = new Set<string>()

  function dfs(id: string): boolean {
    if (done.has(id)) return false
    if (visiting.has(id)) return true
    visiting.add(id)
    for (const dep of graph.get(id) ?? []) {
      if (dfs(dep)) return true
    }
    visiting.delete(id)
    done.add(id)
    return false
  }

  for (const id of graph.keys()) {
    if (dfs(id)) return true
  }
  return false
}

export function defaultConfigForType(type: TransformType): Record<string, unknown> {
  switch (type) {
    case 'FIELD_MAPPER':
      return { fieldMapping: {}, dropUnmapped: false, overwriteExisting: true }
    case 'FILTER':
      return { field: '', operator: 'eq', value: '', keepMatching: true }
    case 'FLATTEN':
      return { fields: '', delimiter: '.' }
    case 'LOOKUP':
      return { dataSourceId: '', lookupTable: '', lookupKey: '', inputKey: '', outputFields: '' }
    case 'SCRIPT':
      return { script: '', language: 'javascript', outputField: 'result' }
    case 'AI_ASSISTED':
      return { prompt: '', outputField: 'ai_output', maxRetries: 3 }
    case 'AGGREGATE':
      return { aggregations: [], groupBy: '' }
    case 'JOIN':
      return { leftKey: '', rightKey: '', joinType: 'inner' }
    case 'SORT':
      return { sortBy: '', direction: 'asc' }
    case 'GROUP':
      return { groupBy: [], outputFormat: 'list' }
    default:
      return {}
  }
}

export function buildDependsOnChain(sorted: Transform[]): Transform[] {
  return sorted.map((t, i) => ({
    ...t,
    dependsOn: i === 0 ? [] : [sorted[i - 1].nodeId]
  }))
}
