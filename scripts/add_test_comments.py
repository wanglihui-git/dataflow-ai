# -*- coding: utf-8 -*-
"""为 src/test 下 Java 测试类补充中文 Javadoc（类、@Test/@BeforeEach 方法）。"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

CLASS_DESC = {
    "AuthControllerTest": "AuthController 的 @WebMvcTest 切片测试，Mock UserService。",
    "UserControllerTest": "UserController 的 WebMvc 测试，含 ADMIN 角色与改密接口。",
    "DataSourceControllerTest": "DataSourceController 测试，Mock 数据源服务与权限。",
    "DataPermissionControllerTest": "DataPermissionController 列/行权限 CRUD 测试。",
    "PipelineControllerTest": "PipelineController CRUD、执行与预览接口测试。",
    "ExecutionControllerTest": "ExecutionController 执行查询、日志与取消测试。",
    "AIControllerTest": "AIController 生成/检索/反馈接口测试。",
    "AuditLogControllerTest": "AuditLogController 管理员审计分页与 403 测试。",
    "GlobalExceptionHandlerTest": "GlobalExceptionHandler 各类异常到 ApiResponse 的映射测试。",
    "TestSecurityConfig": "WebMvcTest 专用精简 Security 配置。",
    "WithMockUserId": "模拟 JWT 过滤器写入的 principal（用户 ID）。",
    "WithMockUserIdSecurityContextFactory": "WithMockUserId 的 SecurityContext 工厂。",
    "ControllerTestAuthSupport": "Controller 测试共用鉴权 Mock 桩。",
    "ApiTestApplication": "@WebMvcTest 锚点配置类，非完整启动。",
    "UserServiceImplTest": "UserServiceImpl 登录、刷新、改密与用户 CRUD 单测。",
    "DataSourceServiceImplTest": "DataSourceServiceImpl 创建、连接测试与预览单测。",
    "PipelineServiceImplTest": "PipelineServiceImpl 创建、分页与预览单测。",
    "ExecutionServiceImplTest": "ExecutionServiceImpl 执行记录与取消单测。",
    "AIServiceImplTest": "AIServiceImpl LLM 生成、向量检索与反馈单测。",
    "AuditLogServiceImplTest": "AuditLogServiceImpl 分页查询单测。",
    "PermissionServiceImplTest": "PermissionServiceImpl 数据源/Pipeline 权限判断单测。",
    "PermissionEngineImplTest": "PermissionEngineImpl 行级过滤与列脱敏单测。",
    "UserRepositoryImplTest": "UserRepositoryImpl 委托 JPA 单测。",
    "DataSourceRepositoryImplTest": "DataSourceRepositoryImpl 委托单测。",
    "PipelineRepositoryImplTest": "PipelineRepositoryImpl 委托与可访问 Pipeline 查询单测。",
    "ExecutionRunRepositoryImplTest": "ExecutionRunRepositoryImpl 统计与查询单测。",
    "AiHelperRepositoryImplTest": "AiHelperRepositoryImpl 向量搜索委托单测（Mock JPA）。",
    "ExponentialBackoffRetryTest": "ExponentialBackoffRetry 重试成功与耗尽场景。",
    "VectorSimilarityUtilsTest": "VectorSimilarityUtils 余弦相似度与阈值校正。",
    "EncryptionServiceTest": "EncryptionService 加解密与 Map 敏感字段单测。",
    "JdbcConnectionTesterTest": "JdbcConnectionTester JDBC 连通性探测单测。",
    "ZhipuClientConfigurationTest": "AiClientConfiguration 智谱 Bean 条件装配测试。",
    "OpenAiCompatibleLlmClientTest": "OpenAiCompatibleLlmClient MockWebServer HTTP 单测。",
    "QianwenLlmClientTest": "QianwenLlmClient MockWebServer 单测。",
    "TransformResponseParserTest": "TransformResponseParser JSON/markdown 解析单测。",
    "OpenAiCompatibleEmbeddingGeneratorTest": "OpenAiCompatibleEmbeddingGenerator MockWebServer 单测。",
    "QianwenEmbeddingGeneratorTest": "QianwenEmbeddingGenerator MockWebServer 单测。",
}


def class_description(name: str) -> str:
    if name in CLASS_DESC:
        return CLASS_DESC[name]
    if name.endswith("Test"):
        return f"{name[:-4]} 单元测试。"
    return f"{name} 测试支撑类。"


def has_javadoc_before(text: str, pos: int) -> bool:
    before = text[:pos].rstrip()
    return before.endswith("*/") or before.endswith("/**")


def insert_class_javadoc(content: str) -> str:
    pattern = re.compile(
        r"^(\s*)((?:@\w+(?:\([^)]*\))?\s*\n\s*)*)((?:public\s+|final\s+)*)class\s+(\w+)",
        re.MULTILINE,
    )

    def repl(m: re.Match) -> str:
        indent, annos, mod1, name = m.group(1), m.group(2), m.group(3), m.group(4)
        start = m.start()
        if has_javadoc_before(content, start):
            return m.group(0)
        desc = class_description(name)
        doc = (
            f"{indent}/**\n"
            f"{indent} * {desc}\n"
            f"{indent} */\n"
        )
        # mod1 已含 public/final 等修饰符，勿在 class_decl 中重复拼接
        return doc + indent + annos + mod1 + f"class {name}"

    return pattern.sub(repl, content, count=1)


def method_doc_from_display(display: str | None, method_name: str) -> str:
    if display:
        return f"验证：{display}。"
    name = method_name
    if name == "setUp":
        return "每个用例执行前初始化 Mock 与测试数据。"
    if name.endswith("_success"):
        return f"验证 {name.replace('_success', '')} 成功场景。"
    if "validation" in name or name.endswith("_fails") or name.endswith("_forbidden"):
        return f"验证 {name} 异常或拒绝场景。"
    return f"测试方法 {method_name}。"


def insert_method_javadocs(content: str) -> str:
    lines = content.split("\n")
    out: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # 收集紧邻的注解块
        if re.match(r"@Test\b|@BeforeEach\b|@AfterEach\b", stripped):
            anno_start = i
            display = None
            while i < len(lines) and (
                lines[i].strip().startswith("@") or lines[i].strip() == ""
            ):
                dm = re.search(r'@DisplayName\("([^"]+)"\)', lines[i])
                if dm:
                    display = dm.group(1)
                i += 1
            # 下一非空应为方法签名
            if i < len(lines) and re.search(r"\bvoid\s+(\w+)\s*\(", lines[i]):
                sig = lines[i]
                mm = re.search(r"\bvoid\s+(\w+)\s*\(", sig)
                method_name = mm.group(1) if mm else "unknown"
                indent = re.match(r"^(\s*)", sig).group(1)

                # 是否已有 javadoc
                prev_idx = len(out) - 1
                has_doc = False
                while prev_idx >= 0 and out[prev_idx].strip() == "":
                    prev_idx -= 1
                if prev_idx >= 0 and out[prev_idx].strip().endswith("*/"):
                    has_doc = True

                if not has_doc:
                    doc_text = method_doc_from_display(display, method_name)
                    out.append(f"{indent}/**")
                    out.append(f"{indent} * {doc_text}")
                    out.append(f"{indent} */")

                for j in range(anno_start, i):
                    out.append(lines[j])
                out.append(sig)
                i += 1
                continue

        # public static 方法（support 类）
        if re.match(r"public\s+static\s+\w+", stripped) and "(" in stripped:
            indent = re.match(r"^(\s*)", line).group(1)
            mm = re.search(r"public\s+static\s+[\w<>,\s]+\s+(\w+)\s*\(", stripped)
            if mm and mm.group(1) not in ("of", "value"):
                prev_idx = len(out) - 1
                has_doc = False
                while prev_idx >= 0 and out[prev_idx].strip() == "":
                    prev_idx -= 1
                if prev_idx >= 0 and out[prev_idx].strip().endswith("*/"):
                    has_doc = True
                if not has_doc and mm.group(1) != "main":
                    out.append(f"{indent}/**")
                    out.append(f"{indent} * 测试辅助：{mm.group(1)}。")
                    out.append(f"{indent} */")

        out.append(line)
        i += 1

    return "\n".join(out)


def add_inline_comments(content: str) -> str:
    """在典型 MockMvc / Mockito 测试方法内添加简要步骤注释。"""
    lines = content.split("\n")
    out: list[str] = []
    in_test_method = False
    brace_depth = 0
    added_arrange = False
    added_act = False

    for line in lines:
        stripped = line.strip()
        if re.search(r"\bvoid\s+\w+\s*\([^)]*\)\s*(throws\s+\w+)?\s*\{", stripped):
            in_test_method = "Test" in "".join(out[-5:]) or "@Test" in content
            brace_depth = 0
            added_arrange = False
            added_act = False

        if in_test_method:
            if "{" in line:
                brace_depth += line.count("{")
            if "}" in line:
                brace_depth -= line.count("}")
                if brace_depth <= 0:
                    in_test_method = False

            if not added_arrange and re.search(r"\bwhen\s*\(", stripped):
                indent = re.match(r"^(\s*)", line).group(1)
                if not (out and "// 准备" in out[-1]):
                    out.append(f"{indent}// 准备：配置 Mock 返回值")
                added_arrange = True

            if not added_act and re.search(r"\bmockMvc\.perform\b", stripped):
                indent = re.match(r"^(\s*)", line).group(1)
                out.append(f"{indent}// 执行：发起 HTTP 请求")
                added_act = True

            if not added_act and re.search(r"\b\w+Service\.\w+\(|aiService\.|encryptionService\.", stripped) and "verify" not in stripped and "when" not in stripped and "assert" not in stripped.lower():
                if re.search(r"(generateTransforms|login|refresh|encrypt|decrypt|findBy)", stripped):
                    indent = re.match(r"^(\s*)", line).group(1)
                    out.append(f"{indent}// 执行：调用被测方法")
                    added_act = True

            if re.search(r"\.andExpect\(|assertEquals|assertNotNull|assertThrows|verify\s*\(", stripped):
                indent = re.match(r"^(\s*)", line).group(1)
                if out and "// 断言" not in "\n".join(out[-3:]):
                    out.append(f"{indent}// 断言：校验响应或交互")
                added_arrange = True  # 避免重复插入

        out.append(line)

    return "\n".join(out)


def process_file(path: Path) -> bool:
    original = path.read_text(encoding="utf-8")
    updated = insert_class_javadoc(original)
    updated = insert_method_javadocs(updated)
    updated = add_inline_comments(updated)
    if updated != original:
        path.write_text(updated, encoding="utf-8")
        return True
    return False


def main():
    roots = [
        ROOT / "dataflow-ai-api/src/test/java",
        ROOT / "dataflow-ai-business/src/test/java",
        ROOT / "dataflow-ai-infrastructure/src/test/java",
    ]
    changed = []
    for root in roots:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*.java")):
            if process_file(path):
                changed.append(path.relative_to(ROOT))

    print(f"Updated {len(changed)} files:")
    for p in changed:
        print(f"  {p}")


if __name__ == "__main__":
    main()
