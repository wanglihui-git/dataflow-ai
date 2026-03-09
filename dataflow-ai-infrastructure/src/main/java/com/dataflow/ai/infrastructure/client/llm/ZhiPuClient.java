package com.dataflow.ai.infrastructure.client.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 智谱AI客户端实现
 */
@Slf4j
@Component
public class ZhiPuClient implements LLMClient {

    @Value("${llm.zhipu.api-key:}")
    private String apiKey;

    @Value("${llm.zhipu.model:glm-4}")
    private String model;

    @Override
    public String generateTransforms(String prompt, Map<String, Object> context) {
        // TODO: 实现智谱AI API调用
        log.info("Generating transforms with ZhipuAI, prompt: {}", prompt);
        return "{}";
    }

    @Override
    public float[] generateEmbedding(String text) {
        // TODO: 实现智谱AI Embedding API调用
        log.info("Generating embedding for text: {}", text);
        return new float[1536];
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean testConnection() {
        // TODO: 实现连接测试
        return !apiKey.isEmpty();
    }
}
