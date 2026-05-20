package com.dataflow.ai.infrastructure.client.embedding;

import com.dataflow.ai.infrastructure.client.llm.LlmApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Embedding 门面，委托给按配置注入的 {@link EmbeddingGenerator}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingGenerator embeddingGenerator;

    public float[] generateEmbedding(String text) {
        return embeddingGenerator.generateEmbedding(text);
    }

    public int getDimensions() {
        return embeddingGenerator.getDimensions();
    }

    public String getModelName() {
        return embeddingGenerator.getModelName();
    }

    /**
     * 计算两个向量的余弦相似度
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
