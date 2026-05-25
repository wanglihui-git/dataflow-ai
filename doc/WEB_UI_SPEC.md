# DataFlow AI — Web 界面开发规格

> 版本：1.0 | 更新：2026-05-25  
> API 基址：`/api`（开发代理 `http://127.0.0.1:7681/api`）  
> 后端对照：[ARCHITECTURE_AND_API.md](./ARCHITECTURE_AND_API.md)

---

## 1. 概述与术语

| 术语 | 说明 |
|------|------|
| Pipeline | Source → Transform(DAG) → Sink 的数据处理管道 |
| Run / Execution | 一次 Pipeline 异步执行记录（`execution_runs`） |
| DataSource | 外部连接配置（加密 JSONB） |
| Transform 节点 | `nodeId`、`type`、`config`、`dependsOn` |
| 混合编辑器 | Vue Flow 画布 + 右侧分类型表单/JSON 配置 |

**产品名称**：数据流转换平台（DataFlow AI）

**已确认决策**：AI 集成于 Pipeline 编辑器抽屉 + 独立「AI 助手」导航；Pipeline 编辑器为混合画布模式。

---

## 2. 设计原则与 Design Tokens

### 2.1 风格

- 中性、现代、信息层级清晰；主内容区浅色，侧边栏 `#1e293b`（slate-800）。
- 参考 `web/src/styles/theme.css` 中的 CSS 变量。

### 2.2 Tokens

| Token | 值 |
|-------|-----|
| `--sidebar-width` | 240px（折叠 64px） |
| `--header-height` | 56px |
| `--content-max-width` | 1440px |
| `--radius-card` | 8px |
| `--shadow-card` | `0 1px 3px rgba(0,0,0,.08)` |
| `--color-success` | `#16a34a` |
| `--color-running` | `#2563eb` |
| `--color-failed` | `#dc2626` |
| `--color-cancelled` | `#6b7280` |
| `--color-pending` | `#ca8a04` |

### 2.3 状态徽章

| 域 | 值 | 颜色语义 |
|----|-----|----------|
| ExecutionRun | PENDING / RUNNING / SUCCESS / FAILED / CANCELLED | 黄 / 蓝 / 绿 / 红 / 灰 |
| Pipeline.status | active / draft / … | active 绿，draft 灰 |
| 测连 | success / failed | 绿 / 红 |

---

## 3. 信息架构与路由

| 路由 | 页面 | 可见性 |
|------|------|--------|
| `/login` | 登录 | 公开 |
| `/` | Dashboard | 已登录 |
| `/pipelines` | Pipeline 列表 | 已登录 |
| `/pipelines/create` | 新建（进编辑器） | 可写角色 |
| `/pipelines/:id` | Pipeline 详情 | 有访问权 |
| `/pipelines/:id/edit` | 混合画布编辑器 | 有修改权 |
| `/data-sources` | 数据源列表 | 已登录 |
| `/data-sources/:id` | 数据源详情 | 已登录 |
| `/executions` | 运行列表 | 已登录 |
| `/executions/:runId` | 运行详情 | 有 Pipeline 访问权 |
| `/ai` | AI 助手 | 已登录 |
| `/users` | 用户管理 | **ADMIN** |
| `/settings` | 设置 | 已登录 |

**侧边栏顺序**：Dashboard → Pipeline → 数据源 → 运行任务 → AI 助手 → User(ADMIN)  
**侧栏顶**：头像占位 + username + role 标签  
**侧栏底**：设置、登出

---

## 4. 全局组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `Layout` | `components/Layout.vue` | 侧栏 + 顶栏 + `<router-view>` |
| `EmptyState` | `components/Common/EmptyState.vue` | 空列表/空画布 |
| `StatusBadge` | `components/Common/StatusBadge.vue` | 统一状态展示 |
| `PageHeader` | `components/Common/PageHeader.vue` | 标题 + 操作区插槽 |

**表格**：行高 ≥ 48px；操作列最小点击区 40×40。  
**表单**：必填红星；`ApiResponse.code !== 200` 时 `ElMessage.error(msg)`。  
**敏感字段**：`type=password` + 显示切换；保存后不请求明文回显。

---

## 5. 分页面规格

### 5.1 登录 `/login`

**布局（自上而下）**

1. 顶栏：Logo +「数据流转换平台」
2. 居中卡片：欢迎登录；username、password；记住我（仅存 username 至 localStorage）；登录按钮
3. 辅助：忘记密码（联系管理员）、注册说明、帮助链接
4. 页脚：版本 + 版权

**API**：`POST /v1/auth/login` → 持久化 token、refreshToken、userId、username、role、department

---

### 5.2 Dashboard `/`

| 区块 | 数据 | 交互 |
|------|------|------|
| 页头 | — | 时间范围 24h/7d/30d；快速新建 Pipeline / DataSource / 运行 |
| KPI×4 | 见 §7.1 | 数字卡片 |
| 趋势图 | execution runs 分桶 | ECharts 折线 SUCCESS vs FAILED |
| 并发 | `status=RUNNING` total | 数字/柱图 |
| 最近运行 | Top 20 | 点击 → Execution 详情 |
| 最近 Pipeline | pipelines size=5 | 点击 → 详情 |

---

### 5.3 Pipeline 列表 `/pipelines`

- 创建、搜索 `name`、筛选 status / permissionLevel / ownerId（后两者客户端）
- 列：name、status、permissionLevel、ownerId、updatedAt、最后运行、操作（查看/编辑/运行/删除）
- 分页：`page`、`size`
- **API**：`GET /v1/pipelines`；`POST .../run`；`DELETE`

---

### 5.4 Pipeline 详情 `/pipelines/:id`

- 上栏：name、status、版本（一期=updatedAt）、编辑/克隆/删除/运行
- 左：描述、权限、负责人、时间戳
- 右：`GET .../runs` 最近 10 条
- 统计：`GET /v1/execution/pipelines/{id}/stats`
- CTA：打开可视化编辑器

**克隆**：`GET` + `POST` 新管道（无专用 clone API）

---

### 5.5 Pipeline 混合编辑器 `/pipelines/:id/edit`

**拓扑**：固定 1 Source + N Transform（DAG）+ 1 Sink

| 区域 | 说明 |
|------|------|
| 左组件库 | 转换 10 类型 + AI 入口；条件/分支标注「规划中」 |
| 中画布 | Vue Flow：网格、缩放、MiniMap；节点显示 type/name |
| 右属性 | 按节点类型动态表单 + JSON 高级模式 |
| 顶栏 | 返回、名称、撤销/重做、保存、保存并发布(active)、运行、导入/导出、校验、预览 |
| 底栏 | 本地变更说明；保存前 updatedAt 冲突警告 |
| AI 抽屉 | generate-transforms / search-similar / feedback |

**校验**：DAG 无环；source.dataSourceId 必填；transform config 非空  
**保存**：`PUT /v1/pipelines/{id}`；draft/active 用 `status` 字段

---

### 5.6 数据源

**列表**：创建、类型筛选、测连缓存状态、预览弹窗  
**详情 Tab**：基本配置 | 连接测试 | 数据预览 | 列权限 | 行权限

---

### 5.7 运行任务

**列表**：Tab 分状态（注意 API 默认 RUNNING）；手动触发 run；取消、重试(再 run)  
**详情**：摘要 metrics；步骤树(phase)；日志轮询 2s；图表静态 metrics；下载日志 JSON

---

### 5.8 用户 `/users`（ADMIN）

用户表 CRUD；详情抽屉；角色子 Tab 静态权限矩阵；审计日志 `GET /v1/audit-logs`

---

### 5.9 AI 助手 `/ai`

对话式指令输入；生成/相似检索/反馈；与编辑器共享 `stores/ai.ts`

---

### 5.10 设置 `/settings`

资料、改密 `PUT /users/me/password`、关于、ADMIN 审计入口

---

## 6. Transform Config Schema 附录

与引擎处理器对齐，表单字段建议如下：

| type | config 字段 | 类型 | 必填 |
|------|-------------|------|------|
| FIELD_MAPPER | fieldMapping | Record<string,string> | 是 |
| FIELD_MAPPER | dropUnmapped, overwriteExisting | boolean | 否 |
| FILTER | condition 或 field+operator+value | string | 是 |
| FILTER | keepMatching | boolean | 否 |
| FLATTEN | fields, delimiter | string | 否 |
| LOOKUP | dataSourceId, lookupTable, lookupKey, inputKey, outputFields | string | 是 |
| SCRIPT | script, language, outputField | string | 是 |
| AI_ASSISTED | prompt, outputField, maxRetries | string/number | 是 |
| AGGREGATE | aggregations[], groupBy | array/string | 是 |
| JOIN | leftKey, rightKey, joinType | string | 是 |
| SORT | sortBy, direction | string | 是 |
| GROUP | groupBy[], outputFormat | array/string | 是 |

**SourceConfig**：dataSourceId, type, tableName?, query?, params?  
**SinkConfig**：dataSourceId, tableName, writeMode, batchSize?, params?  
**ScheduleConfig**：scheduleType(MANUAL|CRON|…), enabled, retryCount, retryInterval, cronExpression?, timezone?

---

## 7. API 映射总表

| 前端模块 | 方法 | 路径 |
|----------|------|------|
| auth.login | POST | /v1/auth/login |
| auth.refresh | POST | /v1/auth/refresh |
| auth.logout | POST | /v1/auth/logout |
| user.list | GET | /v1/users |
| user.get | GET | /v1/users/{id} |
| user.create | POST | /v1/users |
| user.update | PUT | /v1/users/{id} |
| user.delete | DELETE | /v1/users/{id} |
| user.changePassword | PUT | /v1/users/me/password |
| dataSource.* | CRUD | /v1/data-sources |
| dataSource.test | POST | /v1/data-sources/{id}/test |
| dataSource.preview | POST | /v1/data-sources/{id}/preview |
| permissions | GET/POST/DELETE | /v1/data-sources/{id}/column|row-permissions |
| pipeline.* | CRUD | /v1/pipelines |
| pipeline.run | POST | /v1/pipelines/{id}/run |
| pipeline.runs | GET | /v1/pipelines/{id}/runs |
| pipeline.preview | GET | /v1/pipelines/{id}/preview |
| execution.list | GET | /v1/execution/runs |
| execution.get | GET | /v1/execution/runs/{runId} |
| execution.logs | GET | /v1/execution/runs/{runId}/logs |
| execution.cancel | POST | /v1/execution/runs/{runId}/cancel |
| execution.stats | GET | /v1/execution/pipelines/{pipelineId}/stats |
| ai.generate | POST | /v1/ai/generate-transforms |
| ai.search | POST | /v1/ai/search-similar |
| ai.feedback | POST | /v1/ai/feedback |
| audit.list | GET | /v1/audit-logs |

**响应**：`{ code, msg, data }`；分页 `PageResponse { content, page, size, totalElements, totalPages }`

### 7.1 Dashboard KPI 聚合（一期）

| KPI | API |
|-----|-----|
| 用户数 | GET /v1/users（ADMIN） |
| 活跃管道 | GET /v1/pipelines totalElements |
| 24h 成功率 | 拉取 runs 计算 |
| 队列长度 | GET /v1/execution/runs?status=RUNNING |

---

## 8. RBAC 矩阵

| 能力 | ADMIN | DEVELOPER | ANALYST | VIEWER |
|------|:-----:|:---------:|:-------:|:------:|
| 用户管理 | ✓ | — | — | — |
| 创建 Pipeline/DS | ✓ | ✓ | △ | — |
| 运行/取消 | ✓ | △ | △ | — |
| 只读/日志 | ✓ | ✓ | ✓ | ✓ |
| 审计日志 | ✓ | — | — | — |

△ = 依赖 Pipeline `permissionLevel` 与 allowed* 列表

---

## 9. 后端差距与分期

| 需求 | 一期前端策略 | 二期后端建议 |
|------|--------------|--------------|
| Dashboard 聚合 | 多接口并行 | GET /v1/dashboard/summary |
| 忘记密码/注册 | 文案引导 | 邮件重置 / 邀请 token |
| Pipeline 版本 | updatedAt 展示 | 版本表 |
| 标签 | description 代替 | tags[] |
| 执行列表全部 | 多 Tab 请求 | status 可选 ALL |
| 实时日志 | 2s 轮询 | SSE/WebSocket |
| CPU/IO 图表 | metrics 静态 + 占位 | 时序 API |
| 测连历史 | 前端 session | test-history 表 |
| 自定义角色/API Key | 静态矩阵/隐藏 | 新实体 |
| 条件/分支节点 | 标注规划 | 新 TransformType |

---

## 10. 设计快照清单与验收标准

静态 HTML 参考稿：`doc/design-snapshots/`（01–07）

| # | 画面 | 必含元素 |
|---|------|----------|
| 1 | 登录 | 品牌、卡片、页脚 |
| 2 | Dashboard | 4 KPI、趋势图、最近运行表 |
| 3 | Pipeline 列表 | 筛选、表格、分页 |
| 4 | 混合编辑器 | 左库、画布+MiniMap、右属性、AI 抽屉 |
| 5 | Execution 详情 | 步骤树、日志流、metrics |
| 6 | Datasource 详情 | 配置 Tab、列权限 Tab |
| 7 | User 管理 | 用户表、角色矩阵 |

**E2E 冒烟路径**：登录 → 创建数据源 → 创建 Pipeline → 编辑器保存 → 运行 → Execution 详情见日志 → Dashboard 有数据

---

*实现代码位于 `web/` 目录。*
