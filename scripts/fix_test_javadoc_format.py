# -*- coding: utf-8 -*-
"""整理测试类 Javadoc 格式，去除重复行内注释。"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
roots = [
    ROOT / "dataflow-ai-api/src/test/java",
    ROOT / "dataflow-ai-business/src/test/java",
    ROOT / "dataflow-ai-infrastructure/src/test/java",
]

for root in roots:
    for path in root.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        # 修复 /** 与 */ 间多余空行
        text = re.sub(r"/\*\*\s*\n\s*\n\s*\*", "/**\n *", text)
        text = re.sub(r"\n\s*\n\s*\*/", "\n */", text)
        # 连续重复的断言注释只保留第一处
        lines = text.split("\n")
        out = []
        last_was_assert_comment = False
        for line in lines:
            if "// 断言：校验响应或交互" in line:
                if last_was_assert_comment:
                    continue
                last_was_assert_comment = True
            else:
                last_was_assert_comment = False
            out.append(line)
        path.write_text("\n".join(out), encoding="utf-8")

print("format fixed")
