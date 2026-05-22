package com.dataflow.ai.infrastructure.client.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 构建「根据自然语言生成 Pipeline 转换节点」的 LLM 提示词（系统提示 + 用户消息）。
 */
public final class LlmPromptBuilder {

    /** 要求模型仅输出合法 JSON 节点列表的系统提示词 */
    public static final String SYSTEM_PROMPT = """
            You are a data pipeline architect. Given a natural-language instruction and optional schema context, \
            output ONLY valid JSON (no markdown fences) with this shape:
            {
              "nodes": [
                {
                  "nodeId": "unique-id",
                  "type": "FIELD_MAPPER|FILTER|FLATTEN|LOOKUP|SCRIPT|AI_ASSISTED|AGGREGATE|JOIN|SORT|GROUP",
                  "name": "human readable name",
                  "description": "optional",
                  "config": {},
                  "dependsOn": ["other-node-id"]
                }
              ]
            }
            Rules: nodeId and type are required; dependsOn may be empty; at most 10 nodes.
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LlmPromptBuilder() {
    }

    /**
     * 拼装用户侧消息：指令 + 可选上下文 JSON。
     *
     * @param instruction 自然语言指令
     * @param context     表结构、样例数据等，可为空
     * @return 完整 user 消息文本
     */
    public static String buildUserMessage(String instruction, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Instruction: ").append(instruction);
        if (context != null && !context.isEmpty()) {
            sb.append("\n\nContext JSON:\n");
            try {
                // 优先格式化为可读 JSON，失败则回退 toString
                sb.append(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(context));
            } catch (JsonProcessingException e) {
                sb.append(context);
            }
        }
        return sb.toString();
    }
}
