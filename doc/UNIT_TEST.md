# DataFlow AI — 单元测试追踪文档

> 追踪每个 REST 接口在 **Controller → Service → Repository → JPA** 的调用链，并记录各层测试覆盖状态。  
> **范围**：后端 Java 模块（不含 `web/` 前端）。  
> **关联**：[ARCHITECTURE_AND_API.md](./ARCHITECTURE_AND_API.md)、[BACKEND_TODO.md](./BACKEND_TODO.md)  
> **更新日期**：2026-05-21（P2：权限引擎、分页、JWT、审计、执行日志单测）

---

## 1. 文档用途与维护约定

| 约定 | 说明 |
|------|------|
| **API 前缀** | 运行时完整路径 = `/api` + `@RequestMapping`；`@WebMvcTest` 内使用 **`/v1/...`**（不重复 context-path） |
| **状态符号** | ⬜ 未开始 · 🟡 部分/占位 · ✅ 已实现 · ⏸ `@Disabled` 占位 · ➖ 不适用 |
| **测试分层** | C=Controller · S=Service · R=Repository · J=JPA 集成（Testcontainers） |
| **更新时机** | 新增/修改接口或测试类后，同步更新「测试类索引」与覆盖率 |

### 1.1 测试目录（当前）

| 层级 | 路径 |
|------|------|
| Controller | `dataflow-ai-api/src/test/java/com/dataflow/ai/api/controller/` |
| 测试支撑 | `dataflow-ai-api/src/test/java/com/dataflow/ai/api/support/` |
| Service | `dataflow-ai-business/src/test/java/com/dataflow/ai/business/service/impl/` |
| Repository | `dataflow-ai-business/src/test/java/com/dataflow/ai/business/repository/impl/` |
| 集成（规划） | `dataflow-ai-bootstrap/src/test/java/.../integration/` |

> Controller 测试已从 `dataflow-ai-bootstrap/src/test` **迁出**；bootstrap 模块暂无可执行单测。

### 1.2 运行命令

> **重要**：多模块项目不能只 `-pl` 子模块，必须加 **`-am`**（also-make）从根目录构建依赖模块；  
> 否则会报 `Could not find artifact com.dataflow.ai:dataflow-ai-*:jar:1.0-SNAPSHOT`。  
> 请在仓库根目录 `dataflow-ai/` 执行（不要在子模块目录单独 `mvn test`）。

```bash
# 在仓库根目录执行 — Business 单测（推荐）
mvn test -pl dataflow-ai-business -am

# API + Business
mvn test -pl dataflow-ai-api,dataflow-ai-business -am

# 若已全量 install 过，可只测 business（依赖已在本地 .m2）
mvn install -DskipTests
mvn test -pl dataflow-ai-business

# 仅 Controller
mvn test -pl dataflow-ai-api -am -Dtest="*ControllerTest"

# 仅 Service / Repository
mvn test -pl dataflow-ai-business -am -Dtest="*ServiceImplTest,*RepositoryImplTest"

# 全量
mvn test
```

**环境**：需 **JDK 17**（`java -version` 与 `mvn -version` 中的 Java 均为 17）。若 Maven 仍指向 JDK 8，请设置 `JAVA_HOME` 后重开终端。

### 1.3 测试技术栈

- JUnit 5、Mockito、`spring-boot-starter-test`
- `@WebMvcTest` + `TestSecurityConfig` + `@WithMockUserId`（principal 为用户 ID 字符串）
- `@ExtendWith(MockitoExtension.class)`（Service / Repository 委托单测）
- 待补充：Testcontainers PostgreSQL + pgvector（`@Disabled` 用例已预留）

---

## 2. 覆盖率汇总

### 2.1 按接口（28 个业务 API）

| 层级 | 状态 | 说明 |
|------|------|------|
| **Controller** | ✅ **28/28** | 6 个 `*ControllerTest`，Mock Service，覆盖全部端点 |
| **Service** | 🟡 | 7 个 `*ServiceImplTest`；`AIServiceImplTest` 含 LLM JSON 解析断言 |
| **Repository** | 🟡 | 5 个 `*RepositoryImplTest`；Mock JpaRepository 委托；向量/SQL 集成 ⏸ |
| **JPA 集成** | ⬜ | 无 `@DataJpaTest` / Testcontainers |

### 2.2 按模块测试类

| 模块 | 测试类数 | 活跃用例约计 | `@Disabled` |
|------|----------|--------------|-------------|
| dataflow-ai-api | 6 Controller + 2 support | ~30 | 0 |
| dataflow-ai-business | 7 Service + 5 Repository | ~35 | 8 |
| dataflow-ai-bootstrap | 0 | 0 | — |
| dataflow-ai-infrastructure | 4 | ~15 | LLM/Embedding/加密 |

### 2.3 测试类索引

| 测试类 | 模块 | 被测类 | 用例 | 备注 |
|--------|------|--------|------|------|
| `AuthControllerTest` | api | `AuthController` | 2 | login / logout |
| `UserControllerTest` | api | `UserController` | 7 | 含 ADMIN 鉴权 |
| `DataSourceControllerTest` | api | `DataSourceController` | 7 | `@WithMockUserId` |
| `PipelineControllerTest` | api | `PipelineController` | 8 | |
| `ExecutionControllerTest` | api | `ExecutionController` | 3 | |
| `AIControllerTest` | api | `AIController` | 3 | |
| `TestSecurityConfig` | api | — | — | WebMvc 安全配置 |
| `WithMockUserId` | api | — | — | SecurityUtils 兼容 |
| `UserServiceImplTest` | business | `UserServiceImpl` | 4 | login/create |
| `DataSourceServiceImplTest` | business | `DataSourceServiceImpl` | 2+⏸1 | preview ⏸ |
| `PipelineServiceImplTest` | business | `PipelineServiceImpl` | 2+⏸1 | preview ⏸ |
| `ExecutionServiceImplTest` | business | `ExecutionServiceImpl` | 3+⏸1 | async 全链路 ⏸ |
| `AIServiceImplTest` | business | `AIServiceImpl` | 4 | 含 `generateTransforms_parsesLlmResponse` |
| `TransformResponseParserTest` | infrastructure | `TransformResponseParser` | 4 | JSON / markdown |
| `OpenAiCompatibleLlmClientTest` | infrastructure | `OpenAiCompatibleLlmClient` | 4 | MockWebServer |
| `OpenAiCompatibleEmbeddingGeneratorTest` | infrastructure | `EmbeddingGenerator` | 4 | MockWebServer |
| `EncryptionServiceTest` | infrastructure | `EncryptionService` | 4 | 32 字节密钥 |
| `PermissionServiceImplTest` | business | `PermissionServiceImpl` | 4 | 纯逻辑 |
| `AuditLogServiceImplTest` | business | `AuditLogServiceImpl` | 1+⏸1 | |
| `UserRepositoryImplTest` | business | `UserRepositoryImpl` | 3 | |
| `DataSourceRepositoryImplTest` | business | `DataSourceRepositoryImpl` | 2 | |
| `PipelineRepositoryImplTest` | business | `PipelineRepositoryImpl` | 2+⏸1 | SQL 集成 ⏸ |
| `ExecutionRunRepositoryImplTest` | business | `ExecutionRunRepositoryImpl` | 3 | |
| `AiHelperRepositoryImplTest` | business | `AiHelperRepositoryImpl` | 2+⏸1 | pgvector ⏸ |

**说明**：

1. `@WebMvcTest` 路径使用 `/v1/...`，与生产 `/api/v1/...` 等价（仅差 context-path）。
2. 用户不存在等场景仍返回 5xx（`GlobalExceptionHandler` 未实现），Controller 测仅断言 `is5xxServerError()`。
3. 占位实现（数据源 test/preview、Pipeline preview）的 Service 用例仍可能 `@Disabled`；LLM 解析与加密已有 infrastructure/business 单测。

---

## 3. 调用链总览（按 Controller）

```
AuthController          → UserService              → UserRepository
UserController          → UserService              → UserRepository
DataSourceController    → DataSourceService        → DataSourceRepository
PipelineController      → PipelineService          → PipelineRepository
                        → ExecutionService (run)   → ExecutionRunRepository
ExecutionController     → ExecutionService       → ExecutionRunRepository
AIController            → AIService + UserService  → AiHelperRepository + UserRepository
```

**引擎/基础设施（无独立 REST，但被 Service 间接调用）**：

| 组件 | 调用方 | 测试建议 |
|------|--------|----------|
| `PipelineOrchestrator` | `ExecutionServiceImpl.startExecution` | Service 单测 Mock 或引擎集成测 |
| `EncryptionService` | `DataSourceServiceImpl` | Infrastructure 单测 |
| `LLMClient` / `EmbeddingClient` | `AIServiceImpl` | Mock 外部 API |
| `JwtProvider` / `PasswordEncoder` | `UserServiceImpl.login` | Infrastructure 单测 |

---

## 4. 接口追踪矩阵（主表）

> **图例**：C/S/R/J 列为 ⬜/🟡/✅；「建议测试类」为规划命名，便于排期。

### 4.1 认证 — `AuthController`

| # | 方法 | 完整路径 | Controller 方法 | Service | Repository | JPA / 其它 |
|---|------|----------|-----------------|---------|------------|------------|
| A1 | POST | `/api/v1/auth/login` | `login` | `UserService.login` | `findByUsername` → `updateLastLoginAt` | `UserJpaRepository` + `JwtProvider` + `PasswordEncoder` |
| A2 | POST | `/api/v1/auth/logout` | `logout` | ➖ 无 | ➖ 无 | ➖ 仅日志 |

#### A1 调用链明细

```
AuthController.login(LoginRequest)
  └─ UserServiceImpl.login
       ├─ UserRepository.findByUsername(username)
       │    └─ UserRepositoryImpl → UserJpaRepository.findByUsername
       ├─ PasswordEncoder.matches(raw, user.passwordHash)
       ├─ UserServiceImpl.updateLastLogin(userId)
       │    └─ UserRepository.updateLastLoginAt(userId)
       │         └─ UserJpaRepository.updateLastLoginAt(...)
       └─ JwtProvider.generateToken(userId, username, role)
```

| 层级 | 状态 | 测试类 |
|------|------|--------|
| C | ✅ | `AuthControllerTest` |
| S | ✅ | `UserServiceImplTest`（login） |
| R | ✅ | `UserRepositoryImplTest`（委托） |
| J | ⏸ | `UserJpaRepository` 集成待补充 |

#### A2 调用链明细

```
AuthController.logout() → ApiResponse.ofSuccess()  // 无持久化
```

| 层级 | 状态 | 测试类 |
|------|------|--------|
| C | ✅ | `AuthControllerTest.logout_success` |
| S/R/J | ➖ | — |

---

### 4.2 用户 — `UserController`

| # | 方法 | 完整路径 | Controller | Service | Repository |
|---|------|----------|------------|---------|------------|
| U1 | GET | `/api/v1/users` | `list` | `findAllUsers` | `findAll` |
| U2 | GET | `/api/v1/users/{id}` | `get` | `findById` | `findById` |
| U3 | POST | `/api/v1/users` | `create` | `createUser` | `save` |
| U4 | PUT | `/api/v1/users/{id}` | `update` | `updateUser` | `save` |
| U5 | DELETE | `/api/v1/users/{id}` | `delete` | `deleteUser` | `deleteById` |

#### 各接口调用链

**U1 list**（需 `ROLE_ADMIN`）

```
UserController.list → UserServiceImpl.findAllUsers → UserRepository.findAll → UserJpaRepository.findAll
```

**U2 get**

```
UserController.get(id) → UserServiceImpl.findById → UserRepository.findById → UserJpaRepository.findById
  └─ empty → RuntimeException("用户不存在")  // Controller 层
```

**U3 create**（需 `ROLE_ADMIN`）

```
UserController.create(CreateUserRequest)
  → UserServiceImpl.createUser(username, email, password, role, department)
       → UserRepository.save(User) → UserJpaRepository.save
```

> 注意：Controller 传入的是明文 `password`，Service 参数名为 `passwordHash` 且**未**调用 `PasswordEncoder.encode`（实现缺陷，单测应记录期望行为）。

**U4 update**

```
UserController.update(id, User) → user.setId(id) → UserServiceImpl.updateUser → UserRepository.save
```

**U5 delete**

```
UserController.delete(id) → UserServiceImpl.deleteUser → UserRepository.deleteById
```

| # | C | S | R | 测试类 |
|---|---|---|---|--------|
| U1 | ✅ | ✅ | ✅ | `UserControllerTest` / `UserServiceImplTest` / `UserRepositoryImplTest` |
| U2 | ✅ | ✅ | ✅ | 同上 |
| U3 | ✅ | ✅ | ✅ | 同上 |
| U4 | ✅ | ⬜ | ⬜ | C 已覆盖；S/R 随 update 路径补充 |
| U5 | ✅ | ⬜ | ⬜ | C 已覆盖 |

**Service 未暴露但通过 API 间接相关**：

| Service 方法 | 对外 API | 状态 |
|--------------|----------|------|
| `changePassword` | 无 | S/R ⬜（待新增 API 后补测） |
| `findByUsername` | 无（login 内部用） | 随 A1 覆盖 |

---

### 4.3 数据源 — `DataSourceController`

| # | 方法 | 完整路径 | Controller | Service | Repository |
|---|------|----------|------------|---------|------------|
| D1 | POST | `/api/v1/data-sources` | `create` | `createDataSource` | `save` |
| D2 | GET | `/api/v1/data-sources` | `list` | `findByCreatedBy` | `findByCreatedBy` |
| D3 | GET | `/api/v1/data-sources/{id}` | `get` | `findById` | `findById` |
| D4 | PUT | `/api/v1/data-sources/{id}` | `update` | `updateDataSource` | `findById` + `save` |
| D5 | DELETE | `/api/v1/data-sources/{id}` | `delete` | `deleteDataSource` | `deleteById` |
| D6 | POST | `/api/v1/data-sources/{id}/test` | `test` | `testConnection` | `findById` |
| D7 | POST | `/api/v1/data-sources/{id}/preview` | `preview` | `previewSourceData` | `findById` |

#### 调用链（含 Infrastructure）

**D1 create**

```
DataSourceController.create
  → SecurityUtils.getCurrentUserId()
  → DataSourceServiceImpl.createDataSource(request, userId)
       ├─ EncryptionService.encrypt(connectionConfig)
       └─ DataSourceRepository.save → DataSourceJpaRepository.save
```

**D2 list**

```
DataSourceController.list → findByCreatedBy(userId) → DataSourceJpaRepository.findByCreatedBy
```

**D3 get**

```
findById → orElseThrow("数据源不存在")
```

**D4 update**

```
updateDataSource(id, request)
  → findById (empty → RuntimeException)
  → encrypt(optional config) → save
```

**D5 delete** → `deleteById`

**D6 test** → `findById` → `decrypt` → `SourceReaderFactory` → `testConnection()`

**D7 preview** → `findById` → `decrypt` → `SourceReader.preview` → `RecordPreviewMapper`（columns/rows/rowCount）

| # | C | S | R | 测试类 |
|---|---|---|---|--------|
| D1 | ✅ | ✅ | ✅ | `DataSource*Test` |
| D2 | ✅ | ⬜ | ✅ | |
| D3 | ⬜ | ⬜ | ⬜ | C 未单独测 404；可补 |
| D4 | ✅ | ⬜ | ⬜ | |
| D5 | ✅ | ⬜ | ⬜ | |
| D6 | ✅ | ✅ | ⬜ | `DataSourceServiceImplTest`（Mock `SourceReader`） |
| D7 | ✅ | ✅ | ⬜ | 同上 |

**JPA**：`DataSourceJpaRepository` — `findById`, `findByCreatedBy`, `findByType`, `findByName`, `save`, `deleteById`

---

### 4.4 Pipeline — `PipelineController`

| # | 方法 | 完整路径 | Controller | Service | Repository / 其它 |
|---|------|----------|------------|---------|---------------------|
| P1 | POST | `/api/v1/pipelines` | `create` | `createPipeline` | `PipelineRepository.save` |
| P2 | GET | `/api/v1/pipelines` | `list` | `findByUser` | `findAccessibleByUserId` (native SQL) |
| P3 | GET | `/api/v1/pipelines/{id}` | `get` | `findById` | `findById` |
| P4 | PUT | `/api/v1/pipelines/{id}` | `update` | `updatePipeline` | `save` |
| P5 | DELETE | `/api/v1/pipelines/{id}` | `delete` | `deletePipeline` | `deleteById` |
| P6 | POST | `/api/v1/pipelines/{id}/run` | `run` | `executePipeline` | `PipelineRepository.findById` + **`ExecutionService`** |
| P7 | GET | `/api/v1/pipelines/{id}/runs` | `getRuns` | `findExecutionRuns` | **`ExecutionRunRepository.findByPipelineId`** |
| P8 | GET | `/api/v1/pipelines/{id}/preview` | `preview` | `findById` + `previewTransform` | `findById`（preview 无 DB 写） |

#### P6 run 扩展链（重点）

```
PipelineController.run(id)
  → PipelineServiceImpl.executePipeline(pipelineId, userId)
       ├─ PipelineRepository.findById
       ├─ ExecutionServiceImpl.createExecutionRun
       │    └─ ExecutionRunRepository.save  → ExecutionRunJpaRepository.save
       └─ ExecutionServiceImpl.startExecution(runId, pipeline)  [@Async]
            ├─ ExecutionRunRepository.findById / save (状态更新)
            └─ PipelineOrchestrator.execute(context)
                 ├─ SourceReaderFactory / TransformProcessorFactory / SinkWriterFactory
                 └─ DataSourceRepository（经 Reader 间接 findById）
```

| # | C | S | R | 测试类 |
|---|---|---|---|--------|
| P1 | ✅ | ✅ | ✅ | `Pipeline*Test` |
| P2 | ✅ | ⬜ | ✅ | |
| P3 | ⬜ | ⬜ | ⬜ | 可补 404 |
| P4 | ✅ | ⬜ | ⬜ | |
| P5 | ✅ | ⬜ | ⬜ | |
| P6 | ✅ | ✅ | ⬜ | `executePipeline`；`startExecution` ⏸ |
| P7 | ✅ | ⬜ | ✅ | |
| P8 | ✅ | ✅ | ⬜ | `PipelineServiceImplTest` + `PipelinePreviewExecutor` |

**JPA**：

- `PipelineJpaRepository`：`findById`, `findByOwnerId`, `findByPermissionLevel`, `findAccessibleByUserId`, `save`, `deleteById`, `findByName`, `findByStatus`
- `ExecutionRunJpaRepository`（P6/P7）：`save`, `findByPipelineId`, `findById`, `countByPipelineId`, …

---

### 4.5 执行 — `ExecutionController`

| # | 方法 | 完整路径 | Controller | Service | Repository |
|---|------|----------|------------|---------|------------|
| E1 | GET | `/api/v1/execution/runs/{runId}` | `getRun` | `findById` | `findById` |
| E2 | POST | `/api/v1/execution/runs/{runId}/cancel` | `cancel` | `cancelExecution` | `findById` + `save`（状态 CANCELLED） |
| E3 | GET | `/api/v1/execution/pipelines/{pipelineId}/stats` | `getStats` | `getExecutionStats` | `countByPipelineId` ×3 |

#### E2 cancel 链

```
ExecutionServiceImpl.cancelExecution(runId)
  ├─ cancelledFlags / runningContexts（内存）
  └─ updateExecutionStatus → findById + save
```

#### E3 stats 链

```
getExecutionStats(pipelineId)
  ├─ countByPipelineId
  ├─ countByPipelineIdAndStatus(SUCCESS)
  └─ countByPipelineIdAndStatus(FAILED)
  → Map: total, success, failed, successRate
```

| # | C | S | R | 测试类 |
|---|---|---|---|--------|
| E1 | ✅ | ⬜ | ✅ | `Execution*Test` |
| E2 | ✅ | ✅ | ⬜ | |
| E3 | ✅ | ✅ | ✅ | |

---

### 4.6 AI 辅助 — `AIController`

| # | 方法 | 完整路径 | Controller | Service | Repository / 其它 |
|---|------|----------|------------|---------|-------------------|
| AI1 | POST | `/api/v1/ai/generate-transforms` | `generateTransforms` | `AIService.generateTransforms` | `AiHelperRepository.save` + **LLM** + **Embedding** |
| AI2 | POST | `/api/v1/ai/search-similar` | `searchSimilar` | `searchSimilar` | `searchByEmbedding` (native vector SQL) |
| AI3 | POST | `/api/v1/ai/feedback` | `submitFeedback` | `submitFeedback` | `findById` + `save` |

**AI1 额外**：`UserService.findById(currentUserId)` → `UserRepository.findById`

#### AI1 调用链

```
AIController.generateTransforms
  ├─ UserServiceImpl.findById(userId)
  └─ AIServiceImpl.generateTransforms(request, user)
       ├─ LLMClient.generateTransforms(instruction, context)
       ├─ EmbeddingClient.generateEmbedding(instruction)
       └─ AiHelperRepository.save
            └─ AiHelperJpaRepository.save
```

#### AI2 调用链

```
AIServiceImpl.searchSimilar
  ├─ EmbeddingClient.generateEmbedding
  ├─ AiHelperRepository.searchByEmbedding(embedding, minSimilarity, limit)
  │    └─ AiHelperJpaRepository.searchByEmbedding (native, pgvector <=>)
  └─ EmbeddingClient.cosineSimilarity (逐条)
```

#### AI3 调用链

```
AIController.submitFeedback
  ├─ UserService.findById
  └─ AIServiceImpl.submitFeedback
       ├─ AiHelperRepository.findById(aiHelperId)
       └─ AiHelperRepository.save (userFeedback, context.modifiedNodes)
```

| # | C | S | R | 测试类 |
|---|---|---|---|--------|
| AI1 | ✅ | ✅ | ✅ | `AIServiceImplTest`（含 historical_pattern、aiHelperId） |
| AI2 | ✅ | ✅ | ✅ | `AIServiceImplTest.searchSimilar`；`VectorSimilarityUtilsTest` |
| AI3 | ✅ | ✅ | ✅ | accept + `InstructionPattern` upsert；reject/modify 可补 |

**JPA**：`AiHelperJpaRepository` — `findById`, `findByCreatedBy`, `findByPipelineId`, `findWithoutFeedback`, `searchByEmbedding`, `save`, `countByUserFeedback`

---

## 5. Service 方法全覆盖清单（含无 API 方法）

便于 Service 层单测排期，不遗漏「仅有 Service、无 Controller」的方法。

| Service | 方法 | 对外 API | C | S |
|---------|------|----------|---|---|
| **UserService** | `login` | A1 | ⬜ | ⬜ |
| | `findById` | U2, AI* | 🟡 | ⬜ |
| | `findByUsername` | A1 内部 | ⬜ | ⬜ |
| | `createUser` | U3 | 🟡 | ⬜ |
| | `updateUser` | U4 | 🟡 | ⬜ |
| | `deleteUser` | U5 | 🟡 | ⬜ |
| | `findAllUsers` | U1 | 🟡 | ⬜ |
| | `updateLastLogin` | A1 内部 | ⬜ | ⬜ |
| | `changePassword` | 无 | ➖ | ⬜ |
| **DataSourceService** | 全部 8 方法 | D1–D7 | ⬜ | ⬜ |
| **PipelineService** | 全部 11 方法 | P1–P8 等 | ⬜ | ⬜ |
| | `cancelExecution` | 无（ExecutionController 直调 ExecutionService） | ➖ | ⬜ |
| | `updatePipelineStatus` | 无 | ➖ | ⬜ |
| **ExecutionService** | 全部 9 方法 | E1–E3, P6–P7 | ⬜ | ⬜ |
| **AIService** | 3 方法 | AI1–AI3 | ⬜ | ⬜ |
| **PermissionService** | 8 方法 | **无 API** | ➖ | ⬜ |
| **AuditLogService** | 5 方法 | **无 API** | ➖ | ⬜ |

---

## 6. Repository → JPA 映射表

| Repository 接口 | 实现类 | JpaRepository | 需集成测的方法 |
|-----------------|--------|---------------|----------------|
| `UserRepository` | `UserRepositoryImpl` | `UserJpaRepository` | `updateLastLoginAt`（@Modifying） |
| `DataSourceRepository` | `DataSourceRepositoryImpl` | `DataSourceJpaRepository` | JSONB 列读写 |
| `PipelineRepository` | `PipelineRepositoryImpl` | `PipelineJpaRepository` | **`findAccessibleByUserId` 原生 SQL** |
| `ExecutionRunRepository` | `ExecutionRunRepositoryImpl` | `ExecutionRunJpaRepository` | `count*`、`findLatestByPipelineId` |
| `AiHelperRepository` | `AiHelperRepositoryImpl` | `AiHelperJpaRepository` | **`searchByEmbedding` 向量 SQL** |
| `AuditLogRepository` | `AuditLogRepositoryImpl` | `AuditLogJpaRepository` | `deleteByCreatedAtBefore` |
| `FieldPermissionRepository` | `FieldPermissionRepositoryImpl` | `FieldPermissionJpaRepository` | 无 API，权限引擎待实现 |

---

## 7. 建议测试用例（按优先级）

### 7.1 P0 — 补齐现有缺口

| 序号 | 任务 | 关联接口 |
|------|------|----------|
| T1 | 修复并跑通 `UserControllerTest`（路径、JSON body） | U1–U5 |
| T2 | 新增 `AuthControllerTest` | A1, A2 |
| T3 | `UserServiceImplTest`（login/create 密码编码） | A1, U3 |
| T4 | ✅ `AIServiceImplTest` + infrastructure LLM/Embedding 单测 | AI1–AI3 |

### 7.2 P2 — 企业特性（已完成 2026-05-21）

| 序号 | 任务 | 关联 |
|------|------|------|
| T14 | ✅ `PermissionEngineImplTest`、`GlobalExceptionHandler`（集成于 Controller 测试） | TODO-016～020 |
| T15 | ✅ `UserServiceImplTest` refresh token mock | TODO-023～025 |
| T16 | ✅ `PipelineControllerTest` 分页 `PageResponse` | TODO-026 |

### 7.3 P1 — 核心业务 API（已完成 2026-05-21）

| 序号 | 任务 | 关联接口 |
|------|------|----------|
| T5 | ✅ `PipelineControllerTest` + `PipelineServiceImplTest`（含 preview） | P1–P8 |
| T6 | ✅ `ExecutionControllerTest` + `ExecutionServiceImplTest`；鉴权 Mock | E1–E3, P6 |
| T7 | ✅ `DataSourceControllerTest` + `DataSourceServiceImplTest`（test/preview） | D1–D7 |
| T7b | ✅ `AIServiceImplTest`、`VectorSimilarityUtilsTest`、`PermissionServiceImplTest` | AI1–AI3、TODO-015 |
| T7c | ✅ `ControllerTestAuthSupport`（`UserService`/`PermissionService` Mock） | D3–D7、P3–P8、E1–E2 |

### 7.3 P2 — Repository 集成

| 序号 | 任务 | 关联 |
|------|------|------|
| T8 | `@DataJpaTest` + Testcontainers：`AiHelperRepositoryImpl` 向量搜索 | AI2 |
| T9 | `PipelineJpaRepository.findAccessibleByUserId` 权限 SQL | P2 |
| T10 | `ExecutionRunRepository` 统计 count | E3 |

### 7.4 P3 — 引擎与基础设施

| 序号 | 任务 | 关联 |
|------|------|------|
| T11 | `PipelineOrchestratorTest`（Mock Reader/Processor/Writer） | P6 |
| T12 | `JwtProviderTest`；✅ `EncryptionServiceTest` | A1, D1 |
| T13 | `FieldMapperProcessor` 等 Transform 单测 | 引擎 |

---

## 8. 单接口追踪卡（模板）

复制以下模板为每个新接口追加到 PR 或 Issue：

```markdown
### [METHOD] /api/v1/...

**调用链**
- Controller:
- Service:
- Repository:
- JPA / 其它:

**测试状态** C:⬜ S:⬜ R:⬜

**用例**
- [ ]  happy path
- [ ]  404 / 校验失败
- [ ]  鉴权 403
```

---

## 9. 变更日志

| 日期 | 变更 |
|------|------|
| 2026-05-18 | 初版：28 API 全量追踪；登记 `UserControllerTest` 10 用例 |
| 2026-05-18 | 补充 api/business 全层单测；Controller 迁至 `dataflow-ai-api/src/test`；新增 `TestSecurityConfig`、`WithMockUserId`；8 个 `@Disabled` 占位用例 |
| 2026-05-20 | P0：`TransformResponseParserTest`、`OpenAiCompatibleLlmClientTest`、`OpenAiCompatibleEmbeddingGeneratorTest`、`EncryptionServiceTest`；启用 `AIServiceImplTest.generateTransforms_parsesLlmResponse` |
| 2026-05-21 | P1：`DataSourceServiceImplTest`（test/preview）、`PipelineServiceImplTest`（preview）、`AIServiceImplTest`（5 用例）、`VectorSimilarityUtilsTest`、`PermissionServiceImplTest`；Controller 鉴权 `ControllerTestAuthSupport` |
| 2026-05-21 | P2：`PermissionEngineImplTest`、`UserServiceImplTest`（access/refresh）、Pipeline 分页测试；Flyway `V2__p2_pipeline_and_execution.sql` |

---

*完成测试后请将对应单元格从 ⬜ 更新为 ✅，并在「变更日志」记录。*
