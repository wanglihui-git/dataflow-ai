package com.dataflow.ai.infrastructure.client.llm;

import java.util.Map;

/**
 * 大模型客户端接口（Chat Completions）
 */
public interface LLMClient {

    /**
     * 生成转换节点 JSON 文本
     */
    String generateTransforms(String prompt, Map<String, Object> context);

    /**
     * 通用对话补全（自定义 system / user 提示词）
     */
    String complete(String systemPrompt, String userPrompt, Map<String, Object> context);

    /**
     * 使用的模型名称
     */
    String getModelName();

    /**
     * 测试连接
     */
    boolean testConnection();
}
