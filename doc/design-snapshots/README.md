# 设计静态快照参考稿

在浏览器中打开 `01-login.html` … `07-users.html` 查看关键界面线框（1920 布局参考）。

与实现对照：`web/` 源码、`doc/WEB_UI_SPEC.md`。

## 清单

| 文件 | 画面 |
|------|------|
| 01-login.html | 登录 |
| 02-dashboard.html | Dashboard |
| 03-pipeline-list.html | Pipeline 列表 |
| 04-pipeline-editor.html | 混合画布编辑器 + AI 抽屉 |
| 05-execution-detail.html | 运行详情 |
| 06-datasource-detail.html | 数据源详情 |
| 07-users.html | 用户管理 |

## E2E 冒烟

1. 登录 admin
2. 创建数据源并测连
3. 创建 Pipeline → 编辑器保存
4. 运行 → Execution 详情查看日志
5. Dashboard 可见 KPI
