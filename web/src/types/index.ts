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
