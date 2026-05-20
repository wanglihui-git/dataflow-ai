package com.dataflow.ai.infrastructure.client.embedding;

/**
 * 文本向量化生成器（按 app.embedding.provider 切换实现）
 */
public interface EmbeddingGenerator {

    /**
     * 生成文本 embedding 向量
     */
    float[] generateEmbedding(String text);

    /**
     * 向量维度（与库表 vector 列、app.embedding.*.dimensions 一致）
     */
    int getDimensions();

    /**
     * 使用的模型名称
     */
    String getModelName();

    /**
     * 测试 API 连通性
     */
    boolean testConnection();
}
