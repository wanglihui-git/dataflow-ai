export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: string
  username: string
  role: string
  department: string
}

export interface User {
  id: string
  username: string
  email?: string
  role: 'ADMIN' | 'DEVELOPER' | 'ANALYST' | 'VIEWER'
  department?: string
  status?: string
  createdAt?: string
  lastLoginAt?: string
}

export interface SourceConfig {
  type: string
  [key: string]: unknown
}

export interface Transform {
  id: string
  type: string
  name: string
  config: Record<string, unknown>
}

export interface SinkConfig {
  type: string
  [key: string]: unknown
}

export interface ScheduleConfig {
  enabled: boolean
  cron?: string
}

export interface Pipeline {
  id: string
  name: string
  description?: string
  source: SourceConfig
  transforms: Transform[]
  sink: SinkConfig
  schedule?: ScheduleConfig
  ownerId: string
  permissionLevel: 'PRIVATE' | 'SHARED' | 'PUBLIC'
  status: string
  createdAt: string
  updatedAt: string
}

export interface DataSource {
  id: string
  name: string
  type: 'MYSQL' | 'POSTGRES' | 'API' | 'KAFKA' | 'CSV'
  connectionConfig: Record<string, unknown>
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ExecutionRun {
  id: string
  pipelineId: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
  startTime?: string
  endTime?: string
  errorMessage?: string
  executionLog?: Record<string, unknown>
  metrics?: Record<string, unknown>
  triggeredBy?: string
  createdAt: string
}

// ============ 枚举类型 ============

export enum MappingType {
  DIRECT = 'direct',
  CONSTANT = 'constant',
  FUNCTION = 'function',
  ENUMERATE = 'enumerate'
}

export enum TransformType {
  FIELD_MAPPER = 'FIELD_MAPPER',
  FILTER = 'FILTER',
  FLATTEN = 'FLATTEN',
  LOOKUP = 'LOOKUP',
  SCRIPT = 'SCRIPT',
  AI_ASSISTED = 'AI_ASSISTED',
  AGGREGATE = 'AGGREGATE',
  JOIN = 'JOIN',
  SORT = 'SORT',
  GROUP = 'GROUP'
}

export enum ExecutionStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export enum DataSourceType {
  MYSQL = 'MYSQL',
  POSTGRES = 'POSTGRES',
  API = 'API',
  KAFKA = 'KAFKA',
  CSV = 'CSV'
}

export enum AccessType {
  NONE = 'NONE',
  MASKED = 'MASKED',
  FULL = 'FULL'
}

export enum FeedbackType {
  ACCEPT = 'accept',
  MODIFY = 'modify',
  REJECT = 'reject'
}

// ============ 字段映射相关类型 ============

export interface JsonObject {
  type: 'object' | 'array' | 'value'
  name: string
  path: string
  value?: unknown
  valueType?: string
  children?: JsonObject[]
  mapping?: FieldMapping
}

export interface FieldMapping {
  id: string
  sourcePath: string
  targetPath: string
  targetName: string
  mappingType: MappingType
  conditionEnabled: boolean
  conditions?: ConditionBranch[]
  constantValue?: unknown
  functionChain?: FunctionCall[]
  enumeration?: { value: unknown; label: string }[]
}

export interface ConditionBranch {
  id: string
  expression: string
  mappingType: MappingType
  constantValue?: unknown
  functionChain?: FunctionCall[]
  enumeration?: { value: unknown; label: string }[]
}

export interface FunctionCall {
  id: string
  name: string
  params: Record<string, unknown>
  returnType?: string
}

// ============ 内置函数相关类型 ============

export interface FunctionParam {
  name: string
  type: string
  required: boolean
  description: string
  defaultValue?: unknown
}

export interface BuiltinFunction {
  name: string
  displayName: string
  description: string
  category: string
  params: FunctionParam[]
  returnType: string
}

// ============ 执行相关类型 ============

export interface ExecutionStats {
  totalRuns: number
  successfulRuns: number
  failedRuns: number
  avgDuration: number
  successRate: number
  recentTrends: TrendData[]
}

export interface TrendData {
  date: string
  count: number
  duration: number
}

export interface LogEntry {
  timestamp: string
  level: 'INFO' | 'WARN' | 'ERROR'
  message: string
  context?: Record<string, unknown>
}

export interface MetricData {
  recordsProcessed: number
  recordsFailed: number
  throughput: number
  memoryUsage: number
}

// ============ AI 相关类型 ============

export interface SimilarInstruction {
  id: string
  instruction: string
  similarity: number
  transform?: Transform
}

export interface FeedbackData {
  helpful: boolean
  rating?: number
  comment?: string
}

export interface DiagnosisIssue {
  type: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
  message: string
  location?: string
}

export interface DiagnosisResult {
  issues: DiagnosisIssue[]
  suggestions: string[]
  confidence: number
}

// ============ 预览相关类型 ============

export interface PreviewResult {
  transformedData: Record<string, unknown>
  warnings?: string[]
  errors?: TransformationError[]
}

export interface TransformationError {
  path: string
  message: string
  row?: number
}

// ============ 数据源相关类型 ============

export interface ConnectionTestResult {
  success: boolean
  message?: string
  latency?: number
}

export interface PreviewConfig {
  limit?: number
  offset?: number
  query?: string
}

export interface PreviewData {
  columns: string[]
  rows: Record<string, unknown>[]
  total: number
}

// ============ AI 请求相关类型 ============

export interface GenerateContext {
  sourceSchema?: {
    fields: Array<{ name: string; type: string }>
  }
  targetSchema?: {
    fields: Array<{ name: string; type: string }>
  }
  sampleData?: Record<string, unknown>[]
  options?: {
    maxNodes?: number
    strict?: boolean
  }
}

// ============ 用户权限相关类型 ============

export interface DataFieldPermission {
  id: string
  dataSourceId: string
  columnName: string
  targetRole?: string
  targetDepartment?: string
  targetUser?: string
  accessType: AccessType
  maskRule?: string
  createdAt?: string
}
