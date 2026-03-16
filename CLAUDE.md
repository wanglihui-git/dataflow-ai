# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 构建和运行命令

### 构建整个项目
```bash
mvn clean install
```

### 运行应用
```bash
# 从 bootstrap 模块运行
cd dataflow-ai-bootstrap
mvn spring-boot:run

# 或从根目录使用 starter 模块运行
mvn spring-boot:run -pl dataflow-ai-starter
```

### 跳过测试构建
```bash
mvn clean package -DskipTests
```

### 运行测试
```bash
mvn test
```

### 运行指定测试
```bash
mvn test -Dtest=UserControllerTest
```

### 启动后的访问地址
- API: http://localhost:8080/api
- API 文档: http://localhost:8080/api/doc.html
- 健康检查: http://localhost:8080/api/actuator/health
- Actuator 端点: http://localhost:8080/api/actuator

## 项目架构

这是一个采用 DDD（领域驱动设计）分层架构的 Maven 多模块 Spring Boot 3.2.3 项目。

### 模块依赖和职责

```
dataflow-ai-starter        # IDE 执行入口点
dataflow-ai-bootstrap      # Spring Boot 主应用入口 (DataFlowApplication)
    depends on: dataflow-ai-api
dataflow-ai-api            # REST 控制器和拦截器
    depends on: dataflow-ai-business
dataflow-ai-business       # 业务逻辑和服务层
    depends on: dataflow-ai-domain, dataflow-ai-infrastructure
dataflow-ai-infrastructure # 外部集成 (LLM、embedding、datasource)
    depends on: dataflow-ai-domain
dataflow-ai-domain         # 领域实体、DTO、VO、枚举、转换器
dataflow-ai-common         # 共享工具类、常量、异常
```

### 核心架构模式

**Service 层模式**
- Service 接口位于 `dataflow-ai-business/src/main/java/com/dataflow/ai/business/service/`
- Service 实现位于 `dataflow-ai-business/src/main/java/com/dataflow/ai/business/service/impl/`
- 服务通常使用 `@Service` 注解，遵循接口-实现模式

**JPA 自定义转换器**
- 领域实体使用 JPA 注解和自定义 `@Converter` 类处理 JSONB 列
- 转换器位于 `dataflow-ai-domain/src/main/java/com/dataflow/ai/domain/converter/`
- JSONB 列用于存储复杂配置：`SourceConfig`、`Transform` 列表、`SinkConfig`、`ScheduleConfig`

**数据流引擎**
- Pipeline 执行引擎位于 `dataflow-ai-business/src/main/java/com/dataflow/ai/business/engine/`
- Orchestrator 管理 Pipeline 执行流程
- SPI 接口支持可扩展的转换节点

**外部服务客户端**
- LLM 客户端位于 `dataflow-ai-infrastructure/src/main/java/com/dataflow/ai/infrastructure/client/llm/`
- Embedding 客户端位于 `dataflow-ai-infrastructure/src/main/java/com/dataflow/ai/infrastructure/client/embedding/`
- Datasource 客户端位于 `dataflow-ai-infrastructure/src/main/java/com/dataflow/ai/infrastructure/client/datasource/`

## 数据库配置

### PostgreSQL 配合 pgvector 扩展
- 数据库名称: `dataflow_ai` (在 application-dev.yml 中配置)
- 必需扩展: `CREATE EXTENSION IF NOT EXISTS vector;`
- JPA 方言: `PostgreSQLDialect`，使用自定义 `VectorTypeContributor` 支持 pgvector

### Schema 管理
- Flyway 迁移脚本（如果有）放在 `dataflow-ai-bootstrap/src/main/resources/db/migration/`
- 开发环境: `ddl-auto: none` (schema 手动管理)
- 生产环境: `ddl-auto: validate`

### 环境变量
创建 `.env` 文件或设置以下环境变量：
- `DB_USERNAME`, `DB_PASSWORD` - 数据库凭据
- `JWT_SECRET` - JWT 签名密钥，最少 256 位
- `ENCRYPTION_KEY` - 数据加密密钥，32 字节
- `OPENAI_API_KEY` 或 `ZHIPU_API_KEY` - AI 功能所需

## 配置文件

- **dev**: `spring.profiles.active=dev` - 启用调试日志，暴露所有 actuator 端点
- **prod**: `spring.profiles.active=prod` - 生产环境设置

配置文件位置：
- `application-dev.yml` - 开发环境配置
- `application-prod.yml` - 生产环境配置
- `application.yml` - 基础配置

## LLM 和 AI 集成

应用通过 `app.llm.provider` 配置支持两个 LLM 提供商：
- `openai`: 使用 OpenAI API
- `zhipu`: 使用智谱 AI API

向量相似性搜索的 Embedding 也通过 `app.embedding.provider` 支持这两个提供商。

## 核心领域实体

位于 `dataflow-ai-domain/src/main/java/com/dataflow/ai/domain/entity/`：
- `User` - 用户账户，使用 JWT 认证
- `Pipeline` - 数据处理 Pipeline，包含 source、transforms、sink 和 schedule
- `DataSource` - 外部数据源配置
- `ExecutionRun` - Pipeline 执行记录
- `AiHelper` - 存储 AI 生成的转换建议及向量嵌入
- `AuditLog` - 数据访问审计日志

## 开发注意事项

- 需要 Java 17
- 主 Spring Boot 应用类是 bootstrap 模块中的 `com.dataflow.ai.DataFlowApplication`
- starter 模块中的 `Main` 类是用于 IDE 兼容的虚拟入口
- 使用 MapStruct 进行对象映射（转换器在 domain 模块）
- 所有实体使用 Lombok 注解（`@Data`、`@Builder` 等）
- JSON 序列化由 Jackson 处理，时区为 GMT+8
- 文件上传限制: 100MB（可配置）

## 常见问题

- **需要 pgvector 扩展**: 如果使用本地 PostgreSQL，请确保安装并启用了 vector 扩展
- **JWT 密钥长度**: 生产环境至少需要 256 位（32 字节）
- **API 上下文路径**: 所有 API 端点都有 `/api` 前缀
- **端口**: 默认服务器端口为 8080