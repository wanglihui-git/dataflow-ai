package com.dataflow.ai.infrastructure.client.embedding;

/**
 * 文本向量化（Embedding）生成器接口，由 {@code app.embedding.provider} 选择具体实现。
 */
public interface EmbeddingGenerator {

    /**
     * 将单条文本编码为浮点向量。
     *
     * @param text 输入文本，空串时部分实现返回零向量
     * @return embedding 数组
     */
    float[] generateEmbedding(String text);

    /**
     * 向量维度（须与 PostgreSQL vector 列及 {@code app.embedding.*.dimensions} 一致）。
     *
     * @return 维度数
     */
    int getDimensions();

    /**
     * 当前配置的 Embedding 模型名称。
     *
     * @return 模型标识
     */
    String getModelName();

    /**
     * 探测远程 Embedding API 是否可用。
     *
     * @return 成功返回 {@code true}
     */
    boolean testConnection();
}
