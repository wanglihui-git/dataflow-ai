# 为 DataFlow AI 项目添加前端界面

## 背景

项目已有完整的 REST API（`/api` 路径），已配置 Knife4j 文档（`http://localhost:8080/api/doc.html`）。需要为用户提供一个生产级的操作界面。

目标：创建一个长期可维护、功能完善的生产级前端应用。

---

## 方案：React + Ant Design

选择理由：
- React 生态成熟，适合长期维护
- Ant Design 组件丰富，专为中后台系统设计
- TypeScript 支持完善，类型安全
- 社区活跃，文档齐全

---

## 实施步骤

### 1. 创建前端项目

在项目根目录创建 `dataflow-ai-frontend`：

```bash
cd D:\work_home\dataflow-ai
npx create-react-app dataflow-ai-frontend --template typescript
cd dataflow-ai-frontend
npm install antd @ant-design/icons axios react-router-dom dayjs
```

### 2. 项目结构

```
dataflow-ai-frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/              # API 调用模块
│   │   ├── index.ts      # axios 配置
│   │   ├── auth.ts       # 认证 API
│   │   ├── pipeline.ts   # Pipeline API
│   │   ├── dataSource.ts # 数据源 API
│   │   └── execution.ts  # 执行 API
│   ├── components/       # 公共组件
│   │   ├── Layout.tsx    # 主布局
│   │   └── Header.tsx    # 顶部导航
│   ├── pages/            # 页面组件
│   │   ├── Login.tsx     # 登录页
│   │   ├── Dashboard.tsx # 仪表盘
│   │   ├── Pipeline/     # Pipeline 相关页面
│   │   │   ├── List.tsx  # Pipeline 列表
│   │   │   ├── Detail.tsx# Pipeline 详情
│   │   │   └── Create.tsx# 创建 Pipeline
│   │   ├── DataSource/   # 数据源页面

│   │   │   ├── List.tsx
│   │   │   └── Create.tsx
│   │   └── Execution/    # 执行监控页面
│   │       └── List.tsx
│   ├── contexts/         # React Context
│   │   └── AuthContext.tsx
│   ├── types/            # TypeScript 类型定义
│   │   └── index.ts
│   ├── App.tsx
│   ├── index.tsx
│   └── index.css
├── package.json
├── tsconfig.json
└── .env                  # 环境变量
```

### 3. 核心页面设计

| 页面 | 功能 | 对应 API |
|------|------|----------|
| 登录页 | 用户名/密码登录，存储 JWT | POST /api/v1/auth/login |
| 仪表盘 | Pipeline 统计、最近执行记录 | GET /api/v1/pipelines |
| Pipeline 列表 | 查看、搜索、创建、删除 Pipeline | GET/POST/DELETE /api/v1/pipelines |
| Pipeline 详情 | 编辑配置、拖拽调整转换节点、执行 | GET/PUT /api/v1/pipelines/{id} |
| 数据源管理 | 配置数据源、测试连接 | POST /api/v1/data-sources |
| 执行监控 | 实时查看执行状态、日志、取消执行 | GET /api/v1/execution/** |

### 4. 关键实现点

#### API 配置 (`src/api/index.ts`)
- 配置 axios 基础 URL：`http://localhost:8080/api`
- 添加请求拦截器：自动附加 JWT Token
- 添加响应拦截器：处理 401 跳转登录

#### 认证流程 (`src/contexts/AuthContext.tsx`)
- 登录后存储 Token 到 localStorage
- 提供 `login`、`logout`、`isAuthenticated` 方法
- 全局路由守卫保护需要登录的页面

#### Pipeline 编辑器
- 使用 Ant Design 的 `Form` 组件配置 Source、Transform、Sink
- Transform 支持拖拽排序（可使用 `react-beautiful-dnd`）
- 实时预览功能

#### 执行监控
- 使用 WebSocket 或轮询实时更新执行状态
- 实时显示执行日志

### 5. 环境变量配置 (`.env`)
```
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_TITLE=DataFlow AI
```

### 6. 构建和运行

```bash
# 安装依赖
cd dataflow-ai-frontend
npm install

# 开发模式
npm start

# 构建
npm run build

# 预览构建结果
npx serve -s build -l 3000
```

### 7. 部署配置 (推荐：Nginx)

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态资源
    location / {
        root /path/to/dataflow-ai-frontend/build;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
    }
}
```

---

## 需要创建的关键文件

1. `dataflow-ai-frontend/src/api/index.ts` - axios 配置
2. `dataflow-ai-frontend/src/types/index.ts` - TypeScript 类型
3. `dataflow-ai-frontend/src/contexts/AuthContext.tsx` - 认证上下文
4. `dataflow-ai-frontend/src/App.tsx` - 路由配置
5. `dataflow-ai-frontend/src/pages/Login.tsx` - 登录页
6. `dataflow-ai-frontend/src/pages/Pipeline/List.tsx` - Pipeline 列表
7. `dataflow-ai-frontend/src/pages/Pipeline/Detail.tsx` - Pipeline 详情

---

## 验证方法

1. 后端启动：`mvn spring-boot:run`（在 `dataflow-ai-bootstrap` 目录）
2. 前端启动：`npm start`（在 `dataflow-ai-frontend` 目录）
3. 访问：`http://localhost:3000`
4. 测试登录功能（使用已有的测试用户）
5. 测试 Pipeline CRUD 功能
6. 测试 Pipeline 执行和监控功能
