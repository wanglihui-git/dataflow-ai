package com.dataflow.ai.infrastructure.client.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 构建 LLM 生成转换节点的提示词
 */
public final class LlmPromptBuilder {

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

    public static String buildUserMessage(String instruction, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Instruction: ").append(instruction);
        if (context != null && !context.isEmpty()) {
            sb.append("\n\nContext JSON:\n");
            try {
                sb.append(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(context));
            } catch (JsonProcessingException e) {
                sb.append(context);
            }
        }
        return sb.toString();
    }
}
