package com.dataflow.ai.infrastructure.client.embedding;

import com.dataflow.ai.infrastructure.client.llm.LlmApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文本向量化门面：委托给按 {@code app.embedding.provider} 注入的 {@link EmbeddingGenerator}，
 * 并提供本地余弦相似度计算（用于 AI 辅助检索等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingGenerator embeddingGenerator;

    /**
     * 将文本编码为浮点向量。
     *
     * @param text 输入文本
     * @return 与配置维度一致的 embedding
     */
    public float[] generateEmbedding(String text) {
        return embeddingGenerator.generateEmbedding(text);
    }

    /**
     * 当前模型输出的向量维度。
     *
     * @return 维度数
     */
    public int getDimensions() {
        return embeddingGenerator.getDimensions();
    }

    /**
     * 当前使用的 Embedding 模型名称。
     *
     * @return 模型标识
     */
    public String getModelName() {
        return embeddingGenerator.getModelName();
    }

    /**
     * 计算两个等长向量的余弦相似度（范围约 [-1, 1]，零向量时返回 0）。
     *
     * @param vec1 向量一
     * @param vec2 向量二
     * @return 余弦相似度
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            throw new IllegalArgumentException("Vectors must not be null");
        }
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vector dimensions must match: "
                    + vec1.length + " vs " + vec2.length);
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        // 累加点积与各自 L2 范数平方
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
