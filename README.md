# DataFlow AI

AI-powered data transformation platform - 基于 AI 的数据转换平台

## 简介

DataFlow AI 是一个基于 AI 的数据转换平台，通过自然语言指令生成数据处理流程，支持多种数据源和灵活的数据转换配置。平台集成大语言模型（LLM）能力，提供智能化的数据转换建议和相似指令搜索功能。

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.3 |
| Spring Cloud | 2023.0.0 |
| PostgreSQL | 42.7.2 |
| pgvector | 0.1.4 |
| Lombok | 1.18.30 |
| MapStruct | 1.5.5.Final |
| Hutool | 5.8.25 |
| Knife4j | 4.5.0 |
| JWT | 0.11.5 |

## 模块结构

采用 DDD 分层架构设计：

```
dataflow-ai/
├── dataflow-ai-common/      # 公共模块 - 通用工具类、常量、配置
├── dataflow-ai-domain/      # 领域层 - 领域实体、请求/响应对象
├── dataflow-ai-infrastructure/ # 基础设施层 - 数据库访问、外部服务集成
├── dataflow-ai-business/    # 业务逻辑层 - 核心业务服务实现
├── dataflow-ai-api/         # API层 - 控制器、拦截器
├── dataflow-ai-bootstrap/   # 启动模块 - 应用入口、配置
└── dataflow-ai-starter/     # Starter模块 - 自动配置
```

## 核心功能

- **AI 辅助转换**：通过自然语言指令生成数据转换节点
- **相似指令搜索**：基于向量搜索相似的历史指令
- **用户反馈**：收集用户对 AI 生成结果的评价，持续优化
- **数据源管理**：支持多种数据源配置和管理
- **Pipeline 管理**：创建、编辑、删除数据处理流程
- **Pipeline 执行**：执行数据处理任务，支持同步和异步
- **用户认证**：基于 JWT 的用户认证和授权
- **权限管理**：支持数据脱敏和权限控制

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+ (需要安装 pgvector 扩展)

### 安装数据库扩展

```sql
-- 连接到数据库后执行
CREATE EXTENSION IF NOT EXISTS vector;
```

### 配置环境变量

创建 `.env` 文件或设置以下环境变量：

```bash
# 数据库配置
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT 配置
JWT_SECRET=your-secret-key-min-256-bits-long

# 加密配置
ENCRYPTION_KEY=your-encryption-key-32-bytes

# OpenAI 配置
OPENAI_API_KEY=sk-xxx
OPENAI_BASE_URL=https://api.openai.com/v1

# 智谱AI 配置
ZHIPU_API_KEY=your-zhipu-api-key
```

### 启动应用

```bash
# 编译项目
mvn clean install

# 启动应用
cd dataflow-ai-bootstrap
mvn spring-boot:run
```

应用启动后访问：

- API 文档: http://localhost:8080/api/doc.html
- 健康检查: http://localhost:8080/api/actuator/health

## API 文档

### AI 辅助接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/ai/generate-transforms` | POST | AI 生成转换节点 |
| `/api/v1/ai/search-similar` | POST | 搜索相似指令 |
| `/api/v1/ai/feedback` | POST | 提交 AI 反馈 |

### 其他接口

- 认证接口：`/api/v1/auth/*`
- 用户接口：`/api/v1/users/*`
- 数据源接口：`/api/v1/datasources/*`
- Pipeline 接口：`/api/v1/pipelines/*`
- 执行接口：`/api/v1/executions/*`

## 配置说明

### LLM 配置

支持两种 LLM 提供商：

```yaml
app:
  llm:
    provider: openai  # 或 zhipu
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL}
      model: gpt-4
    zhipu:
      api-key: ${ZHIPU_API_KEY}
      model: glm-4
```

### Embedding 配置

用于向量搜索的 Embedding 配置：

```yaml
app:
  embedding:
    provider: openai
    openai:
      model: text-embedding-3-small
      dimensions: 1536
```

## 开发指南

### 代码规范

- 遵循阿里巴巴 Java 开发规范
- 使用 Lombok 减少样板代码
- 使用 MapStruct 进行对象映射

### 运行测试

```bash
mvn test
```

### 构建 Docker 镜像

```bash
mvn clean package -DskipTests
docker build -t dataflow-ai:latest .
```

## 许可证

[MIT License](LICENSE)

## 联系方式

如有问题或建议，请提交 Issue。
