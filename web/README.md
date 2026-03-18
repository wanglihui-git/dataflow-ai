# DataFlow AI Web

DataFlow AI 前端项目

## 技术栈

- Vue 3 (Composition API)
- TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- Axios

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:5173

### 构建

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

## 项目结构

```
src/
├── api/              # API 调用模块
├── components/       # 公共组件
├── pages/            # 页面组件
├── router/           # 路由配置
├── stores/           # Pinia 状态管理
├── types/            # TypeScript 类型定义
├── App.vue           # 根组件
└── main.ts           # 应用入口
```

## 功能模块

- 用户登录/登出
- 仪表盘统计
- Pipeline 管理（列表、详情、执行）
- 数据源管理（列表、创建、测试连接）
