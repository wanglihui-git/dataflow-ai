package com.dataflow.ai.infrastructure.client.llm;

import java.util.Map;

/**
 * 大语言模型（LLM）客户端统一接口，供 Pipeline AI 辅助、对话补全等场景使用。
 */
public interface LLMClient {

    /**
     * 根据自然语言指令与上下文，生成 Pipeline 转换节点 JSON 文本。
     *
     * @param prompt  用户指令
     * @param context 可选 schema/样例等上下文
     * @return LLM 返回的 JSON 字符串
     */
    String generateTransforms(String prompt, Map<String, Object> context);

    /**
     * 通用对话补全（自定义 system / user 提示词）。
     *
     * @param systemPrompt 系统提示词，可为空
     * @param userPrompt   用户提示词
     * @param context      预留扩展上下文，部分实现可忽略
     * @return 模型回复正文
     */
    String complete(String systemPrompt, String userPrompt, Map<String, Object> context);

    /**
     * 当前配置使用的模型名称。
     *
     * @return 模型标识
     */
    String getModelName();

    /**
     * 探测 API 密钥与端点是否可用（通常发送极简请求）。
     *
     * @return 连通返回 {@code true}
     */
    boolean testConnection();
}
