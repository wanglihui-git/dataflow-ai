export type UserRole = 'ADMIN' | 'DEVELOPER' | 'ANALYST' | 'VIEWER'
export type ExecutionStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
export type DataSourceType = 'MYSQL' | 'POSTGRES' | 'API' | 'KAFKA' | 'CSV'
export type TransformType =
  | 'FIELD_MAPPER'
  | 'FILTER'
  | 'FLATTEN'
  | 'LOOKUP'
  | 'SCRIPT'
  | 'AI_ASSISTED'
  | 'AGGREGATE'
  | 'JOIN'
  | 'SORT'
  | 'GROUP'
export type PermissionLevel = 'PRIVATE' | 'SHARED' | 'PUBLIC'

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface LoginResult {
  token: string
  refreshToken: string
  userId: string
  username: string
  role: UserRole
  department?: string
}

export interface UserVO {
  id: string
  username: string
  email?: string
  role: UserRole
  department?: string
  status?: string
  createdAt?: string
  lastLoginAt?: string
}

export interface SourceConfig {
  dataSourceId?: string
  type?: DataSourceType
  tableName?: string
  query?: string
  params?: Record<string, unknown>
}

export interface SinkConfig {
  dataSourceId?: string
  tableName?: string
  writeMode?: 'APPEND' | 'OVERWRITE' | 'IGNORE_DUPLICATES' | 'UPDATE_EXISTING'
  batchSize?: number
  params?: Record<string, unknown>
}

export interface Transform {
  nodeId: string
  type: TransformType
  name?: string
  description?: string
  config?: Record<string, unknown>
  dependsOn?: string[]
  generatedBy?: string
}

export interface ScheduleConfig {
  scheduleType?: 'MANUAL' | 'FIXED_RATE' | 'FIXED_DELAY' | 'CRON'
  cronExpression?: string
  interval?: number
  timezone?: string
  enabled?: boolean
  retryCount?: number
  retryInterval?: number
}

export interface Pipeline {
  id: string
  name: string
  description?: string
  source?: SourceConfig
  transforms?: Transform[]
  sink?: SinkConfig
  schedule?: ScheduleConfig
  ownerId?: string
  permissionLevel?: PermissionLevel
  allowedRoles?: string[]
  allowedUsers?: string[]
  allowedDepartments?: string[]
  status?: string
  createdAt?: string
  updatedAt?: string
}

export interface DataSource {
  id: string
  name: string
  type: DataSourceType
  connectionConfig?: Record<string, unknown>
  ownerId?: string
  status?: string
  createdAt?: string
  updatedAt?: string
}

export interface ExecutionRun {
  id: string
  pipelineId: string
  status: ExecutionStatus
  startTime?: string
  endTime?: string
  errorMessage?: string
  executionLog?: { entries?: LogEntry[] }
  metrics?: Record<string, unknown>
  triggeredBy?: string
  createdAt?: string
}

export interface LogEntry {
  timestamp?: string
  phase?: string
  message?: string
  level?: string
}

export interface AuditLog {
  id: string
  userId?: string
  action?: string
  resourceType?: string
  resourceId?: string
  createdAt?: string
}
