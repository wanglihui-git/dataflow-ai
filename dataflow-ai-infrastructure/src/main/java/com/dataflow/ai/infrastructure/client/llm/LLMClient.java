package com.dataflow.ai.infrastructure.client.llm;

import java.util.Map;

/**
 * 大模型客户端接口
 * 支持多模型实现：OpenAI、智谱、文心等
 */
public interface LLMClient {

    /**
     * 生成转换节点
     * @param prompt 提示词
     * @param context 上下文（schema、样本数据等）
     * @return 生成的JSON字符串
     */
    String generateTransforms(String prompt, Map<String, Object> context);

    /**
     * 生成Embedding向量
     * @param text 输入文本
     * @return 向量数组
     */
    float[] generateEmbedding(String text);

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 测试连接
     */
    boolean testConnection();
}