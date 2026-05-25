# -*- coding: utf-8 -*-
"""Regenerate API sections (§10+) in doc/ARCHITECTURE_AND_API.md with full JSON samples."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOC = ROOT / "doc" / "ARCHITECTURE_AND_API.md"
BASE = "http://127.0.0.1:8080/api"
T = "$token"

WRAPPER = '''```json
{{
  "code": 200,
  "msg": "Success",
  "data": {data}
}}
```'''

VOID = WRAPPER.format(data="null")
BOOL_TRUE = WRAPPER.format(data="true")


def wrap(inner: str) -> str:
    inner = inner.strip()
    if inner in ("null", "true", "false"):
        return WRAPPER.format(data=inner)
    return f"```json\n{{\n  \"code\": 200,\n  \"msg\": \"Success\",\n  \"data\": {inner}\n}}\n```"


def cg(path, auth=False, q=""):
    qs = f"?{q}" if q else ""
    h = f' -H "Authorization: Bearer {T}"' if auth else ""
    return f'```powershell\ncurl "{BASE}{path}{qs}"{h}\n```'


def cp(path, body=None, auth=False, q=""):
    qs = f"?{q}" if q else ""
    h = ' -H "Content-Type: application/json"'
    if auth:
        h += f' -H "Authorization: Bearer {T}"'
    d = f" -d '{body}'" if body else ""
    return f'```powershell\ncurl.exe -X POST "{BASE}{path}{qs}"{h}{d}\n```'


def cu(path, body, q=""):
    qs = f"?{q}" if q else ""
    return (
        f'```powershell\ncurl.exe -X PUT "{BASE}{path}{qs}" '
        f'-H "Content-Type: application/json" -H "Authorization: Bearer {T}" '
        f"-d '{body}'\n```"
    )


def cd(path):
    return f'```powershell\ncurl.exe -X DELETE "{BASE}{path}" -H "Authorization: Bearer {T}"\n```'


def block(method, path, desc, auth, perm, params, resp_json, ex1, ex2=None):
    a = "否" if not auth else "是（Bearer JWT）"
    lines = [
        f"#### {method} `{path}`", "",
        "| 项 | 内容 |", "|----|------|",
        f"| **说明** | {desc} |",
        f"| **认证** | {a} |",
        f"| **权限** | {perm or '—'} |", "",
    ]
    if params:
        lines += ["**参数**", "", params, ""]
    lines += ["**响应体**", "", resp_json, "", "**示例**", "", ex1, ""]
    if ex2:
        lines += [ex2, ""]
    return "\n".join(lines)


# --- JSON data payloads (inner only) ---
LOGIN = """{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": "u-001",
    "username": "admin",
    "role": "ADMIN",
    "department": "IT"
  }"""

USER_VO = """{
    "id": "u-002",
    "username": "dev1",
    "email": "dev1@corp.com",
    "role": "DEVELOPER",
    "department": "RD",
    "status": "active",
    "createdAt": "2026-05-20 10:00:00",
    "lastLoginAt": "2026-05-22 09:30:00"
  }"""

USERS_LIST = f"[{USER_VO}]"

DATASOURCE = """{
    "id": "ds-001",
    "name": "mysql-demo",
    "type": "MYSQL",
    "connectionConfig": {
      "host": "ENC(...)",
      "port": 3306,
      "database": "demo",
      "username": "ENC(...)",
      "password": "ENC(...)"
    },
    "createdBy": "u-001",
    "createdAt": "2026-05-20 10:00:00",
    "updatedAt": "2026-05-22 08:00:00"
  }"""

DS_LIST = f"[{DATASOURCE}]"

PREVIEW = """{
    "columns": ["id", "order_no", "amount"],
    "rows": [
      {"id": 1, "order_no": "A001", "amount": 99.5},
      {"id": 2, "order_no": "A002", "amount": 120.0}
    ],
    "rowCount": 2,
    "sampleSize": 10
  }"""

COL_PERM = """{
    "id": "cp-001",
    "dataSourceId": "ds-001",
    "columnName": "salary",
    "targetRole": "ANALYST",
    "targetDepartment": null,
    "targetUser": null,
    "accessType": "MASKED",
    "maskRule": "****",
    "createdAt": "2026-05-21 12:00:00"
  }"""

ROW_PERM = """{
    "id": "rp-001",
    "dataSourceId": "ds-001",
    "targetRole": "VIEWER",
    "targetDepartment": "Sales",
    "targetUser": null,
    "filterCondition": "dept_id = 'SALES'",
    "priority": 10,
    "createdAt": "2026-05-21 12:00:00"
  }"""

PIPELINE = """{
    "id": "pl-001",
    "name": "etl-orders",
    "description": "订单清洗",
    "source": {
      "dataSourceId": "ds-001",
      "type": "MYSQL",
      "tableName": "orders"
    },
    "transforms": [
      {
        "nodeId": "t1",
        "type": "FIELD_MAPPER",
        "name": "字段映射",
        "config": {},
        "dependsOn": []
      }
    ],
    "sink": {
      "dataSourceId": "ds-001",
      "tableName": "orders_clean",
      "writeMode": "APPEND",
      "batchSize": 1000
    },
    "schedule": {
      "scheduleType": "MANUAL",
      "enabled": false,
      "retryCount": 3,
      "retryInterval": 60
    },
    "ownerId": "u-001",
    "permissionLevel": "PRIVATE",
    "status": "active",
    "createdAt": "2026-05-20 10:00:00",
    "updatedAt": "2026-05-22 08:00:00"
  }"""

PAGE_PIPELINE = f"""{{
    "content": [{PIPELINE}],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }}"""

EXEC_RUN = """{
    "id": "run-001",
    "pipelineId": "pl-001",
    "status": "RUNNING",
    "startTime": "2026-05-22 10:00:00",
    "endTime": null,
    "errorMessage": null,
    "executionLog": {
      "entries": [
        {
          "timestamp": "2026-05-22T10:00:01",
          "phase": "INIT",
          "message": "Execution started"
        }
      ]
    },
    "metrics": {
      "recordsProcessed": 0,
      "sourceDurationMs": 0
    },
    "triggeredBy": "u-001",
    "createdAt": "2026-05-22 10:00:00"
  }"""

EXEC_RUN_SUCCESS = EXEC_RUN.replace('"RUNNING"', '"SUCCESS"').replace(
    '"endTime": null', '"endTime": "2026-05-22 10:01:30"'
).replace('"recordsProcessed": 0', '"recordsProcessed": 1000')

EXEC_RUNS_LIST = f"[{EXEC_RUN}]"

PAGE_EXEC = f"""{{
    "content": [{EXEC_RUN}],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }}"""

LOG_ENTRIES = """[
    {
      "timestamp": "2026-05-22T10:00:01",
      "phase": "SOURCE",
      "message": "Read 1000 records"
    },
    {
      "timestamp": "2026-05-22T10:00:45",
      "phase": "SINK",
      "message": "Wrote 1000 records"
    }
  ]"""

STATS = """{
    "total": 10,
    "success": 8,
    "failed": 2,
    "successRate": 0.8
  }"""

PREVIEW_PL = """{
    "sampleSize": 10,
    "inputRecordCount": 10,
    "outputRecordCount": 10,
    "records": [
      {"id": 1, "amount": 99.5, "amount_mapped": 99.5}
    ]
  }"""

AI_GENERATE = """{
    "aiHelperId": "ah-001",
    "source": {
      "type": "llm_generated",
      "confidence": 0.92,
      "matchedInstruction": null
    },
    "nodes": [
      {
        "nodeId": "t1",
        "type": "FIELD_MAPPER",
        "name": "金额映射",
        "config": {
          "mappings": [{"source": "amt", "target": "amount"}]
        },
        "dependsOn": [],
        "generatedBy": "ah-001"
      }
    ],
    "suggestions": [
      {"type": "info", "message": "建议检查源字段类型是否为 decimal"}
    ],
    "visualization": {
      "summary": "1 个映射节点",
      "dataFlow": "amt -> amount"
    },
    "metadata": {
      "processingTimeMs": 1250,
      "modelUsed": "qwen-plus"
    }
  }"""

AI_SEARCH = """{
    "results": [
      {
        "instruction": "把金额字段映射为 amount",
        "similarity": 0.91,
        "useCount": 12,
        "acceptanceRate": 0.75,
        "generatedNodes": [
          {
            "nodeId": "t1",
            "type": "FIELD_MAPPER",
            "name": "金额映射",
            "config": {}
          }
        ]
      }
    ]
  }"""

AUDIT_PAGE = """{
    "content": [
      {
        "id": 1001,
        "userId": "u-001",
        "action": "LOGIN",
        "resourceType": "USER",
        "resourceId": "u-001",
        "details": {"ip": "127.0.0.1"},
        "ipAddress": "127.0.0.1",
        "userAgent": "curl/8.0",
        "createdAt": "2026-05-22 09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }"""

HEALTH = '```json\n{"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}\n```'


def gen_apis():
    o = []
    o.append("### 11.1 认证（AuthController）\n\n基础路径：`/v1/auth`\n\n")
    o.append(block("POST", "/v1/auth/login", "校验用户名密码，返回 JWT 与 refreshToken。", False, "—",
        "| 位置 | 名称 | 类型 | 必填 | 说明 |\n|------|------|------|------|------|\n| Body | username | string | 是 | 用户名 |\n| Body | password | string | 是 | 密码 |",
        wrap(LOGIN), cp("/v1/auth/login", '{"username":"admin","password":"admin123"}'),
        '```powershell\n$token = (curl.exe -s -X POST "http://127.0.0.1:8080/api/v1/auth/login" -H "Content-Type: application/json" -d "{\\"username\\":\\"admin\\",\\"password\\":\\"admin123\\"}" | ConvertFrom-Json).data.token\n```'))
    o.append(block("POST", "/v1/auth/refresh", "使用 refreshToken 换取新的 token 对。", False, "—",
        "| Body | refreshToken | string | 是 | 刷新令牌 |", wrap(LOGIN),
        cp("/v1/auth/refresh", '{"refreshToken":"<refresh-token>"}'),
        cp("/v1/auth/login", '{"username":"admin","password":"admin123"}')))
    o.append(block("POST", "/v1/auth/logout", "无状态登出；客户端清除 token。", False, "—", "无。", VOID,
        cp("/v1/auth/logout"), cg("/v1/auth/logout", True)))

    o.append("\n### 11.2 用户（UserController）\n\n基础路径：`/v1/users`\n\n")
    o.append(block("GET", "/v1/users", "查询全部用户（不含密码）。", True, "ROLE_ADMIN", "无。",
        wrap(USERS_LIST), cg("/v1/users", True), cg("/v1/users", True)))
    o.append(block("GET", "/v1/users/{id}", "按 ID 查询用户。", True, "已登录",
        "| Path | id | string | 是 | 用户 ID |", wrap(USER_VO),
        cg("/v1/users/u-002", True), cg("/v1/users/u-002", True)))
    o.append(block("PUT", "/v1/users/me/password", "修改当前登录用户密码。", True, "已登录",
        "| Body | oldPassword | string | 是 |\n| Body | newPassword | string | 是 |", VOID,
        cu("/v1/users/me/password", '{"oldPassword":"admin123","newPassword":"NewPass123!"}'),
        cg("/v1/users/me/password", True)))
    o.append(block("POST", "/v1/users", "创建用户。", True, "ROLE_ADMIN",
        "| Body | username, email, password, role, department? | `CreateUserRequest` |", wrap(USER_VO),
        cp("/v1/users", '{"username":"dev1","email":"dev1@corp.com","password":"pass1234","role":"DEVELOPER","department":"RD"}', True),
        cp("/v1/users", '{"username":"dev1","email":"dev1@corp.com","password":"pass1234","role":"DEVELOPER"}', True)))
    o.append(block("PUT", "/v1/users/{id}", "更新用户；Path id 覆盖 Body id。", True, "ROLE_ADMIN",
        "| Path | id | Body: User JSON |", wrap(USER_VO),
        cu("/v1/users/u-002", '{"username":"dev1","email":"dev1@corp.com","role":"ANALYST","status":"active"}'),
        cu("/v1/users/u-002", '{"department":"Platform"}')))
    o.append(block("DELETE", "/v1/users/{id}", "删除用户。", True, "ROLE_ADMIN", "| Path | id |", VOID,
        cd("/v1/users/u-002"), cd("/v1/users/u-999")))

    o.append("\n### 11.3 数据源（DataSourceController）\n\n基础路径：`/v1/data-sources`\n\n")
    ds_create = '{"name":"mysql-demo","type":"MYSQL","connectionConfig":{"host":"127.0.0.1","port":3306,"database":"demo","username":"root","password":"secret"}}'
    o.append(block("POST", "/v1/data-sources", "创建数据源，连接配置加密存储。", True, "已登录",
        "| Body | name, type, connectionConfig | `CreateDataSourceRequest` |", wrap(DATASOURCE),
        cp("/v1/data-sources", ds_create, True), cp("/v1/data-sources", ds_create, True)))
    o.append(block("GET", "/v1/data-sources", "列出当前用户创建的数据源。", True, "已登录", "无。", wrap(DS_LIST),
        cg("/v1/data-sources", True), cg("/v1/data-sources", True)))
    o.append(block("GET", "/v1/data-sources/{id}", "数据源详情。", True, "资源访问权", "| Path | id |", wrap(DATASOURCE),
        cg("/v1/data-sources/ds-001", True), cg("/v1/data-sources/ds-001", True)))
    o.append(block("PUT", "/v1/data-sources/{id}", "部分更新。", True, "可修改权",
        "| Body | name?, type?, connectionConfig? |", wrap(DATASOURCE),
        cu("/v1/data-sources/ds-001", '{"name":"mysql-demo-v2"}'),
        cu("/v1/data-sources/ds-001", '{"connectionConfig":{"host":"10.0.0.1"}}')))
    o.append(block("DELETE", "/v1/data-sources/{id}", "删除数据源。", True, "可修改权", "| Path | id |", VOID,
        cd("/v1/data-sources/ds-001"), cd("/v1/data-sources/ds-001")))
    o.append(block("POST", "/v1/data-sources/{id}/test", "测试连通性。", True, "访问权", "| Path | id |", BOOL_TRUE,
        cp("/v1/data-sources/ds-001/test", auth=True), cp("/v1/data-sources/ds-001/test", auth=True)))
    o.append(block("POST", "/v1/data-sources/{id}/preview", "预览源数据样本。", True, "访问权",
        "| Query | tableName?, query?, sampleSize?（默认 10） |", wrap(PREVIEW),
        cg("/v1/data-sources/ds-001/preview", True, "tableName=orders&sampleSize=5"),
        cp("/v1/data-sources/ds-001/preview", auth=True, q="query=SELECT%20*%20FROM%20orders%20LIMIT%205")))

    o.append("\n### 11.4 数据权限（DataPermissionController）\n\n")
    col_body = '{"columnName":"salary","targetRole":"ANALYST","accessType":"MASKED","maskRule":"****"}'
    row_body = '{"targetRole":"VIEWER","filterCondition":"dept_id = \'SALES\'","priority":10}'
    o.append(block("GET", "/v1/data-sources/{dataSourceId}/column-permissions", "列权限列表。", True, "数据源访问",
        "| Path | dataSourceId |", wrap(f"[{COL_PERM}]"),
        cg("/v1/data-sources/ds-001/column-permissions", True), cg("/v1/data-sources/ds-001/column-permissions", True)))
    o.append(block("POST", "/v1/data-sources/{dataSourceId}/column-permissions", "创建列权限。", True, "可修改数据源",
        "| Body | DataFieldPermission |", wrap(COL_PERM),
        cp("/v1/data-sources/ds-001/column-permissions", col_body, True), cp("/v1/data-sources/ds-001/column-permissions", col_body, True)))
    o.append(block("DELETE", "/v1/data-sources/{dataSourceId}/column-permissions/{id}", "删除列权限。", True, "可修改",
        "| Path | dataSourceId, id |", VOID, cd("/v1/data-sources/ds-001/column-permissions/cp-001"), cd("/v1/data-sources/ds-001/column-permissions/cp-001")))
    o.append(block("GET", "/v1/data-sources/{dataSourceId}/row-permissions", "行权限列表。", True, "访问",
        "| Path | dataSourceId |", wrap(f"[{ROW_PERM}]"),
        cg("/v1/data-sources/ds-001/row-permissions", True), cg("/v1/data-sources/ds-001/row-permissions", True)))
    o.append(block("POST", "/v1/data-sources/{dataSourceId}/row-permissions", "创建行过滤。", True, "可修改",
        "| Body | DataRowPermission |", wrap(ROW_PERM),
        cp("/v1/data-sources/ds-001/row-permissions", row_body, True), cp("/v1/data-sources/ds-001/row-permissions", row_body, True)))
    o.append(block("DELETE", "/v1/data-sources/{dataSourceId}/row-permissions/{id}", "删除行权限。", True, "可修改",
        "| Path | id |", VOID, cd("/v1/data-sources/ds-001/row-permissions/rp-001"), cd("/v1/data-sources/ds-001/row-permissions/rp-001")))

    o.append("\n### 11.5 Pipeline（PipelineController）\n\n基础路径：`/v1/pipelines`\n\n")
    pl_create = '{"name":"etl-orders","description":"订单清洗","source":{"dataSourceId":"ds-001","type":"MYSQL","tableName":"orders"},"transforms":[],"sink":{"dataSourceId":"ds-001","tableName":"orders_clean","writeMode":"APPEND"},"schedule":{"scheduleType":"MANUAL","enabled":false}}'
    o.append(block("POST", "/v1/pipelines", "创建 Pipeline。", True, "已登录",
        "| Body | `CreatePipelineRequest` |", wrap(PIPELINE),
        cp("/v1/pipelines", pl_create, True), cp("/v1/pipelines", pl_create, True)))
    o.append(block("GET", "/v1/pipelines", "分页查询可访问 Pipeline。", True, "已登录",
        "| Query | name?, page（0）, size（20） |", wrap(PAGE_PIPELINE),
        cg("/v1/pipelines", True), cg("/v1/pipelines", True, "name=etl&page=0&size=10")))
    o.append(block("GET", "/v1/pipelines/{id}", "Pipeline 详情。", True, "访问权", "| Path | id |", wrap(PIPELINE),
        cg("/v1/pipelines/pl-001", True), cg("/v1/pipelines/pl-001", True)))
    o.append(block("PUT", "/v1/pipelines/{id}", "更新 Pipeline。", True, "修改权", "| Body | Pipeline |", wrap(PIPELINE),
        cu("/v1/pipelines/pl-001", '{"name":"etl-orders-v2","status":"active"}'),
        cu("/v1/pipelines/pl-001", '{"description":"updated"}')))
    o.append(block("DELETE", "/v1/pipelines/{id}", "删除 Pipeline。", True, "删除权", "| Path | id |", VOID,
        cd("/v1/pipelines/pl-001"), cd("/v1/pipelines/pl-001")))
    o.append(block("POST", "/v1/pipelines/{id}/run", "异步触发执行。", True, "执行权", "| Path | id |",
        wrap(EXEC_RUN.replace('"RUNNING"', '"PENDING"')),
        cp("/v1/pipelines/pl-001/run", auth=True), cp("/v1/pipelines/pl-001/run", auth=True)))
    o.append(block("GET", "/v1/pipelines/{id}/runs", "该 Pipeline 执行历史。", True, "访问权", "| Path | id |",
        wrap(EXEC_RUNS_LIST), cg("/v1/pipelines/pl-001/runs", True), cg("/v1/pipelines/pl-001/runs", True)))
    o.append(block("GET", "/v1/pipelines/{id}/preview", "采样预览转换（sampleSize=10）。", True, "访问权", "| Path | id |",
        wrap(PREVIEW_PL), cg("/v1/pipelines/pl-001/preview", True), cg("/v1/pipelines/pl-001/preview", True)))

    o.append("\n### 11.6 执行（ExecutionController）\n\n基础路径：`/v1/execution`\n\n")
    o.append(block("GET", "/v1/execution/runs", "按状态分页查询执行记录。", True, "已登录",
        "| Query | status?（未传默认 RUNNING）, page, size |", wrap(PAGE_EXEC),
        cg("/v1/execution/runs", True), cg("/v1/execution/runs", True, "status=SUCCESS&page=0&size=20")))
    o.append(block("GET", "/v1/execution/runs/{runId}", "执行详情。", True, "Pipeline 访问权", "| Path | runId |",
        wrap(EXEC_RUN_SUCCESS), cg("/v1/execution/runs/run-001", True), cg("/v1/execution/runs/run-001", True)))
    o.append(block("GET", "/v1/execution/runs/{runId}/logs", "执行日志 entries。", True, "访问权", "| Path | runId |",
        wrap(LOG_ENTRIES), cg("/v1/execution/runs/run-001/logs", True), cg("/v1/execution/runs/run-001/logs", True)))
    o.append(block("POST", "/v1/execution/runs/{runId}/cancel", "取消运行中任务。", True, "执行权", "| Path | runId |", VOID,
        cp("/v1/execution/runs/run-001/cancel", auth=True), cp("/v1/execution/runs/run-001/cancel", auth=True)))
    o.append(block("GET", "/v1/execution/pipelines/{pipelineId}/stats", "执行统计。", True, "访问权", "| Path | pipelineId |",
        wrap(STATS), cg("/v1/execution/pipelines/pl-001/stats", True), cg("/v1/execution/pipelines/pl-001/stats", True)))

    o.append("\n### 11.7 AI 辅助（AIController）\n\n基础路径：`/v1/ai`\n\n")
    ai_gen = '{"instruction":"把金额字段映射为 amount","context":{"sourceSchema":{"fields":[{"name":"amt","type":"decimal"}]}},"options":{"maxNodes":5,"strict":true}}'
    o.append(block("POST", "/v1/ai/generate-transforms", "LLM 生成节点并写入 ai_helpers。", True, "已登录",
        "| Body | `GenerateTransformsRequest` |", wrap(AI_GENERATE),
        cp("/v1/ai/generate-transforms", ai_gen, True), cp("/v1/ai/generate-transforms", ai_gen, True)))
    o.append(block("POST", "/v1/ai/search-similar", "向量检索相似指令。", True, "已登录",
        "| Body | instruction, limit?, minSimilarity? |", wrap(AI_SEARCH),
        cp("/v1/ai/search-similar", '{"instruction":"映射金额","limit":5,"minSimilarity":0.75}', True),
        cp("/v1/ai/search-similar", '{"instruction":"映射金额"}', True)))
    o.append(block("POST", "/v1/ai/feedback", "accept/modify/reject 反馈。", True, "已登录",
        "| Body | aiHelperId, action, modifiedNodes?, pipelineId? |", VOID,
        cp("/v1/ai/feedback", '{"aiHelperId":"ah-001","action":"accept","pipelineId":"pl-001"}', True),
        cp("/v1/ai/feedback", '{"aiHelperId":"ah-001","action":"reject"}', True)))

    o.append("\n### 11.8 审计日志（AuditLogController）\n\n基础路径：`/v1/audit-logs`\n\n")
    o.append(block("GET", "/v1/audit-logs", "分页查询审计日志。", True, "ROLE_ADMIN",
        "| Query | userId?, action?, start?, end?（ISO-8601）, page, size |", wrap(AUDIT_PAGE),
        cg("/v1/audit-logs", True), cg("/v1/audit-logs", True, "userId=u-001&action=LOGIN&page=0&size=20")))

    return "\n".join(o)


def gen_tail():
    return f"""
## 12. Actuator 与文档端点

### 12.1 Spring Boot Actuator

基础路径：`/api/actuator`

#### GET `/actuator/health`

| 项 | 内容 |
|----|------|
| **说明** | 应用健康检查 |
| **认证** | 否 |

**响应体**（Spring Boot 标准格式，非 `ApiResponse`）：

{HEALTH}

**示例**

```powershell
curl "http://127.0.0.1:8080/api/actuator/health"
```

---

#### GET `/actuator/info`

| 项 | 内容 |
|----|------|
| **说明** | 应用信息 |
| **认证** | 是 |

**响应体**（节选）：

```json
{{
  "app": {{
    "name": "dataflow-ai",
    "version": "1.0-SNAPSHOT"
  }}
}}
```

**示例**

```powershell
curl "http://127.0.0.1:8080/api/actuator/info" -H "Authorization: Bearer $token"
```

```powershell
curl "http://127.0.0.1:8080/api/actuator/info" -H "Authorization: Bearer $token"
```

---

#### GET `/actuator/metrics`

| 项 | 内容 |
|----|------|
| **说明** | 指标名列表；`/{{name}}` 查看单项 |
| **认证** | 是 |

**响应体**（`GET /actuator/metrics`，节选）：

```json
{{
  "names": [
    "jvm.memory.used",
    "dataflow.pipeline.records.processed",
    "dataflow.pipeline.execution.duration"
  ]
}}
```

**示例**

```powershell
curl "http://127.0.0.1:8080/api/actuator/metrics" -H "Authorization: Bearer $token"
```

```powershell
curl "http://127.0.0.1:8080/api/actuator/metrics/dataflow.pipeline.records.processed" -H "Authorization: Bearer $token"
```

---

#### GET `/actuator/prometheus`

| 项 | 内容 |
|----|------|
| **说明** | Prometheus 文本格式 |
| **认证** | 是 |

**响应体**（`text/plain` 节选）：

```
# HELP dataflow_pipeline_records_processed_total Records processed
dataflow_pipeline_records_processed_total 1000.0
```

**示例**

```powershell
curl "http://127.0.0.1:8080/api/actuator/prometheus" -H "Authorization: Bearer $token"
```

---

### 12.2 API 文档（Knife4j / Swagger）

| 地址 | 说明 |
|------|------|
| http://127.0.0.1:8080/api/doc.html | Knife4j UI |
| http://127.0.0.1:8080/api/swagger-ui.html | Swagger UI |
| http://127.0.0.1:8080/api/v3/api-docs | OpenAPI JSON |

**示例**

```powershell
curl "http://127.0.0.1:8080/api/v3/api-docs"
```

```powershell
start http://127.0.0.1:8080/api/doc.html
```

---

## 13. 附录：枚举与值对象

### 13.1 SourceConfig

```json
{{
  "dataSourceId": "ds-uuid",
  "type": "MYSQL",
  "tableName": "orders",
  "query": "SELECT * FROM orders WHERE dt = '2026-01-01'",
  "params": {{}}
}}
```

### 13.2 SinkConfig

```json
{{
  "dataSourceId": "ds-uuid",
  "tableName": "orders_clean",
  "writeMode": "APPEND",
  "batchSize": 1000,
  "params": {{}}
}}
```

`writeMode`：`APPEND` | `OVERWRITE` | `IGNORE_DUPLICATES` | `UPDATE_EXISTING`

### 13.3 Transform

```json
{{
  "nodeId": "t1",
  "type": "FIELD_MAPPER",
  "name": "字段映射",
  "description": "",
  "config": {{ "mappings": [] }},
  "dependsOn": [],
  "generatedBy": "ai-helper-id"
}}
```

### 13.4 ScheduleConfig

```json
{{
  "scheduleType": "MANUAL",
  "cronExpression": "0 0 * * * ?",
  "interval": 3600,
  "timezone": "Asia/Shanghai",
  "enabled": false,
  "retryCount": 3,
  "retryInterval": 60
}}
```

`scheduleType`：`MANUAL` | `FIXED_RATE` | `FIXED_DELAY` | `CRON`

### 13.5 ExecutionRun.status

`PENDING` | `RUNNING` | `SUCCESS` | `FAILED` | `CANCELLED`

### 13.6 接口总览（与 §11.0 一致）

共 **39** 个业务 REST + **4** 个 Actuator，详见 [§11.0](#110-接口总览)。

---

## 变更与已知差异

| 项 | 说明 |
|----|------|
| README 路径 | 部分文档写 `/datasources`；实际为 `data-sources` |
| 执行列表默认状态 | `GET /v1/execution/runs` 未传 `status` 时默认 `RUNNING` |
| 在线文档 | 以本文 + Knife4j 为准 |

---

*文档与源码同步维护（2026-05-22）。交互式调试：http://127.0.0.1:8080/api/doc.html*
"""


def main():
    head = DOC.read_text(encoding="utf-8").split("## 10. 统一响应格式")[0]
    intro = """## 10. 统一响应格式

所有 Controller 返回 `ApiResponse<T>`（JSON）：

```json
{
  "code": 200,
  "msg": "Success",
  "data": { }
}
```

| HTTP 状态 | code（body） | 场景 |
|-----------|--------------|------|
| 200 | 200 | 成功 |
| 400 | 400 | `@Valid` 校验失败 |
| 401 | — | 无效/缺失 JWT |
| 500 | 500 | 未捕获异常 |
| 200 | 403/404/409 | `BusinessException`（HTTP 200，body 带业务 code） |

`GlobalExceptionHandler` 统一处理校验与业务异常。

**分页** `PageResponse<T>`：`content`, `page`, `size`, `totalElements`, `totalPages`。

---

## 11. REST API 详细说明

> **基址**：`http://127.0.0.1:8080/api`  
> **鉴权**：除标注「认证：否」外，需 `Authorization: Bearer <JWT>`。  
> 下列示例为 PowerShell `curl`；登录后使用 `$token`。

### 11.0 接口总览

| # | 方法 | 路径 | 认证 | 说明 |
|---|------|------|------|------|
| 1 | POST | /v1/auth/login | 否 | 登录 |
| 2 | POST | /v1/auth/refresh | 否 | 刷新令牌 |
| 3 | POST | /v1/auth/logout | 否 | 登出 |
| 4 | GET | /v1/users | 是 | ADMIN 列表 |
| 5 | GET | /v1/users/{id} | 是 | 用户详情 |
| 6 | PUT | /v1/users/me/password | 是 | 改密 |
| 7 | POST | /v1/users | 是 | ADMIN 创建 |
| 8 | PUT | /v1/users/{id} | 是 | ADMIN 更新 |
| 9 | DELETE | /v1/users/{id} | 是 | ADMIN 删除 |
| 10 | POST | /v1/data-sources | 是 | 创建数据源 |
| 11 | GET | /v1/data-sources | 是 | 列表 |
| 12 | GET | /v1/data-sources/{id} | 是 | 详情 |
| 13 | PUT | /v1/data-sources/{id} | 是 | 更新 |
| 14 | DELETE | /v1/data-sources/{id} | 是 | 删除 |
| 15 | POST | /v1/data-sources/{id}/test | 是 | 测连 |
| 16 | POST | /v1/data-sources/{id}/preview | 是 | 预览 |
| 17 | GET | /v1/data-sources/{dataSourceId}/column-permissions | 是 | 列权限列表 |
| 18 | POST | /v1/data-sources/{dataSourceId}/column-permissions | 是 | 创建列权限 |
| 19 | DELETE | /v1/data-sources/{dataSourceId}/column-permissions/{id} | 是 | 删除列权限 |
| 20 | GET | /v1/data-sources/{dataSourceId}/row-permissions | 是 | 行权限列表 |
| 21 | POST | /v1/data-sources/{dataSourceId}/row-permissions | 是 | 创建行权限 |
| 22 | DELETE | /v1/data-sources/{dataSourceId}/row-permissions/{id} | 是 | 删除行权限 |
| 23 | POST | /v1/pipelines | 是 | 创建 Pipeline |
| 24 | GET | /v1/pipelines | 是 | Pipeline 分页 |
| 25 | GET | /v1/pipelines/{id} | 是 | 详情 |
| 26 | PUT | /v1/pipelines/{id} | 是 | 更新 |
| 27 | DELETE | /v1/pipelines/{id} | 是 | 删除 |
| 28 | POST | /v1/pipelines/{id}/run | 是 | 执行 |
| 29 | GET | /v1/pipelines/{id}/runs | 是 | 执行历史 |
| 30 | GET | /v1/pipelines/{id}/preview | 是 | 预览转换 |
| 31 | GET | /v1/execution/runs | 是 | 分页查询执行 |
| 32 | GET | /v1/execution/runs/{runId} | 是 | 执行详情 |
| 33 | GET | /v1/execution/runs/{runId}/logs | 是 | 执行日志 |
| 34 | POST | /v1/execution/runs/{runId}/cancel | 是 | 取消 |
| 35 | GET | /v1/execution/pipelines/{pipelineId}/stats | 是 | 统计 |
| 36 | POST | /v1/ai/generate-transforms | 是 | AI 生成 |
| 37 | POST | /v1/ai/search-similar | 是 | 相似指令 |
| 38 | POST | /v1/ai/feedback | 是 | 反馈 |
| 39 | GET | /v1/audit-logs | 是 | ADMIN 审计 |
| 40 | GET | /actuator/health | 否 | 健康检查 |
| 41 | GET | /actuator/info | 是 | 应用信息 |
| 42 | GET | /actuator/metrics | 是 | 指标 |
| 43 | GET | /actuator/prometheus | 是 | Prometheus |

**合计：39 业务 REST + 4 Actuator。**

---

"""
    DOC.write_text(head + intro + gen_apis() + gen_tail(), encoding="utf-8")
    print("OK", DOC, "lines", len((head + intro + gen_apis()).splitlines()))


if __name__ == "__main__":
    main()
