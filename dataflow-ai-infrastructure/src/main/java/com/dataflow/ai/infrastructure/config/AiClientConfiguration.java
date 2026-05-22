package com.dataflow.ai.infrastructure.config;

import com.dataflow.ai.infrastructure.client.embedding.EmbeddingGenerator;
import com.dataflow.ai.infrastructure.client.embedding.OpenAiCompatibleEmbeddingGenerator;
import com.dataflow.ai.infrastructure.client.embedding.QianwenEmbeddingGenerator;
import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import com.dataflow.ai.infrastructure.client.llm.OpenAiCompatibleLlmClient;
import com.dataflow.ai.infrastructure.client.llm.QianwenLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AI 客户端 Spring 配置：按 {@code app.llm.provider}、{@code app.embedding.provider}
 * 条件注册通义千问、OpenAI、智谱等 LLM 与 Embedding 实现 Bean。
 */
@Configuration
public class AiClientConfiguration {

    // --- LLM ---

    /**
     * 默认 LLM 客户端：通义千问 DashScope（{@code app.llm.provider=qianwen} 或未配置时生效）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.llm.provider", havingValue = "qianwen", matchIfMissing = true)
    public LLMClient qianwenLlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.llm.qianwen.api-key:}") String apiKey,
            @Value("${app.llm.qianwen.base-url:" + QianwenLlmClient.DEFAULT_ENDPOINT + "}") String baseUrl,
            @Value("${app.llm.qianwen.model:qwen-plus}") String model,
            @Value("${app.llm.qianwen.max-tokens:4000}") int maxTokens,
            @Value("${app.llm.qianwen.temperature:0.7}") double temperature) {
        return new QianwenLlmClient(webClientBuilder, apiKey, baseUrl, model, maxTokens, temperature);
    }

    /**
     * OpenAI 兼容 Chat Completions 客户端（{@code app.llm.provider=openai}）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai")
    public LLMClient openAiLlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.llm.openai.api-key:}") String apiKey,
            @Value("${app.llm.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.llm.openai.model:gpt-4}") String model,
            @Value("${app.llm.openai.max-tokens:4000}") int maxTokens,
            @Value("${app.llm.openai.temperature:0.7}") double temperature) {
        return new OpenAiCompatibleLlmClient(webClientBuilder, apiKey, baseUrl, model, maxTokens, temperature, "OpenAI");
    }

    /**
     * 智谱 AI OpenAI 兼容客户端（{@code app.llm.provider=zhipu}）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.llm.provider", havingValue = "zhipu")
    public LLMClient zhipuLlmClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.llm.zhipu.api-key:}") String apiKey,
            @Value("${app.llm.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${app.llm.zhipu.model:glm-4}") String model,
            @Value("${app.llm.zhipu.max-tokens:4000}") int maxTokens,
            @Value("${app.llm.zhipu.temperature:0.7}") double temperature) {
        return new OpenAiCompatibleLlmClient(webClientBuilder, apiKey, baseUrl, model, maxTokens, temperature, "Zhipu");
    }

    // --- Embedding ---

    /**
     * 默认 Embedding 生成器：通义千问（{@code app.embedding.provider=qianwen} 或未配置时生效）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "qianwen", matchIfMissing = true)
    public EmbeddingGenerator qianwenEmbeddingGenerator(
            WebClient.Builder webClientBuilder,
            @Value("${app.embedding.qianwen.api-key:}") String apiKey,
            @Value("${app.embedding.qianwen.base-url:" + QianwenEmbeddingGenerator.DEFAULT_ENDPOINT + "}") String baseUrl,
            @Value("${app.embedding.qianwen.model:text-embedding-v3}") String model,
            @Value("${app.embedding.qianwen.dimensions:1024}") int dimensions) {
        return new QianwenEmbeddingGenerator(webClientBuilder, apiKey, baseUrl, model, dimensions);
    }

    /**
     * OpenAI Embeddings 生成器（{@code app.embedding.provider=openai}）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "openai")
    public EmbeddingGenerator openAiEmbeddingGenerator(
            WebClient.Builder webClientBuilder,
            @Value("${app.embedding.openai.api-key:}") String apiKey,
            @Value("${app.embedding.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.embedding.openai.model:text-embedding-3-small}") String model,
            @Value("${app.embedding.openai.dimensions:1536}") int dimensions) {
        return new OpenAiCompatibleEmbeddingGenerator(webClientBuilder, baseUrl, apiKey, model, dimensions, "OpenAI");
    }

    /**
     * 智谱 Embeddings 生成器（{@code app.embedding.provider=zhipu}）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "zhipu")
    public EmbeddingGenerator zhipuEmbeddingGenerator(
            WebClient.Builder webClientBuilder,
            @Value("${app.embedding.zhipu.api-key:}") String apiKey,
            @Value("${app.embedding.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${app.embedding.zhipu.model:embedding-2}") String model,
            @Value("${app.embedding.zhipu.dimensions:1024}") int dimensions) {
        return new OpenAiCompatibleEmbeddingGenerator(webClientBuilder, baseUrl, apiKey, model, dimensions, "Zhipu");
    }
}
