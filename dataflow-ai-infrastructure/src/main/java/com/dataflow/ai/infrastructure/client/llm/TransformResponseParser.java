package com.dataflow.ai.infrastructure.client.llm;

import com.dataflow.ai.domain.exception.BusinessException;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.domain.response.ResponseCode;
import com.dataflow.ai.domain.vo.Transform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 将 LLM 返回的 JSON 文本解析为领域对象 {@link Transform} 节点列表。
 * <p>兼容根对象为 {@code {"nodes":[...]}} 或直接为数组；自动剥离 Markdown 代码围栏。
 */
@Component
public class TransformResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 解析 LLM 响应为转换节点列表。
     *
     * @param llmResponse 模型返回的 JSON 字符串
     * @return 转换节点列表
     * @throws BusinessException 响应为空或 JSON 结构不合法时
     */
    public List<Transform> parse(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            throw new BusinessException(ResponseCode.CODE_400, "LLM returned empty response");
        }
        // 1. 去掉 ```json 围栏  2. 定位 nodes 数组  3. 逐节点映射为 Transform
        String json = stripMarkdownFences(llmResponse.trim());
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode nodesNode = root.has("nodes") ? root.get("nodes") : root;
            if (!nodesNode.isArray()) {
                throw new BusinessException(ResponseCode.CODE_400,
                        "LLM response must contain a JSON array field 'nodes'");
            }
            List<Transform> transforms = new ArrayList<>();
            Iterator<JsonNode> it = nodesNode.elements();
            while (it.hasNext()) {
                transforms.add(parseNode(it.next()));
            }
            return transforms;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.CODE_400,
                    "Failed to parse LLM JSON into transform nodes: " + e.getMessage());
        }
    }

    private Transform parseNode(JsonNode node) {
        String nodeId = textOrNull(node, "nodeId", "id");
        if (nodeId == null || nodeId.isBlank()) {
            throw new BusinessException(ResponseCode.CODE_400, "Transform node missing nodeId");
        }
        String typeStr = textOrNull(node, "type");
        if (typeStr == null || typeStr.isBlank()) {
            throw new BusinessException(ResponseCode.CODE_400, "Transform node " + nodeId + " missing type");
        }
        TransformType type;
        try {
            // 枚举名不区分大小写
            type = TransformType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCode.CODE_400,
                    "Unknown transform type: " + typeStr + " for node " + nodeId);
        }
        Map<String, Object> config = null;
        if (node.has("config") && !node.get("config").isNull()) {
            config = MAPPER.convertValue(node.get("config"), MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        }
        List<String> dependsOn = Collections.emptyList();
        if (node.has("dependsOn") && node.get("dependsOn").isArray()) {
            dependsOn = new ArrayList<>();
            for (JsonNode dep : node.get("dependsOn")) {
                dependsOn.add(dep.asText());
            }
        }
        return Transform.builder()
                .nodeId(nodeId)
                .type(type)
                .name(textOrNull(node, "name"))
                .description(textOrNull(node, "description"))
                .config(config)
                .dependsOn(dependsOn)
                .build();
    }

    private static String textOrNull(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            if (node.has(field) && !node.get(field).isNull()) {
                String v = node.get(field).asText();
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }

    static String stripMarkdownFences(String text) {
        if (text.startsWith("```")) {
            // 跳过首行 ``` 或 ```json
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            // 截断末尾 ```
            int endFence = text.lastIndexOf("```");
            if (endFence >= 0) {
                text = text.substring(0, endFence);
            }
            return text.trim();
        }
        return text;
    }
}
