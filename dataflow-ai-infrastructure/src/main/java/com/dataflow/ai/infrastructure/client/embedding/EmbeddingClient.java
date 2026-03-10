package com.dataflow.ai.infrastructure.client.embedding;

import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Embedding向量客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    @Resource(name = "openAIClient")
    private LLMClient llmClient;

    /**
     * 生成文本的Embedding向量
     */
    public float[] generateEmbedding(String text) {
        try {
            return llmClient.generateEmbedding(text);
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            return new float[1536];
        }
    }

    /**
     * 计算两个向量的余弦相似度
     */
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
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
