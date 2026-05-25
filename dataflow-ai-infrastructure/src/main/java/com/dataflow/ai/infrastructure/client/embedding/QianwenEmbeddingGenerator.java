package com.dataflow.ai.infrastructure.client.embedding;

import com.dataflow.ai.infrastructure.client.llm.LlmApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 通义千问 DashScope 文本向量 API 客户端。
 * <p>默认使用 OpenAI 兼容端点 {@code .../compatible-mode/v1/embeddings}，
 * 响应为 {@code data[0].embedding}；原生端点响应为 {@code output.embeddings[0].embedding}。
 */
@Slf4j
public class QianwenEmbeddingGenerator implements EmbeddingGenerator {

    /** 默认 DashScope 文本向量完整 URL */
    public static final String DEFAULT_ENDPOINT =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String endpointUrl;
    private final String model;
    private final int dimensions;

    /**
     * @param webClientBuilder WebClient 构建器
     * @param apiKey           DashScope API Key
     * @param endpointUrl      完整向量接口 URL，空则使用 {@link #DEFAULT_ENDPOINT}
     * @param model            模型名
     * @param dimensions       向量维度（写入 parameters.dimension）
     */
    public QianwenEmbeddingGenerator(
            WebClient.Builder webClientBuilder,
            String apiKey,
            String endpointUrl,
            String model,
            int dimensions) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.endpointUrl = endpointUrl == null || endpointUrl.isBlank() ? DEFAULT_ENDPOINT : endpointUrl.trim();
        this.model = model;
        this.dimensions = dimensions;
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public float[] generateEmbedding(String text) {
        if (apiKey.isBlank()) {
            throw new LlmApiException("Qianwen embedding API key is not configured");
        }
        if (text == null || text.isBlank()) {
            return new float[dimensions];
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        if (isCompatibleModeEndpoint()) {
            body.put("input", text);
            if (dimensions > 0) {
                body.put("dimensions", dimensions);
            }
        } else {
            ObjectNode input = body.putObject("input");
            ArrayNode texts = input.putArray("texts");
            texts.add(text);
            if (dimensions > 0) {
                ObjectNode parameters = body.putObject("parameters");
                parameters.put("dimension", dimensions);
            }
        }

        try {
            String response = webClient.post()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);
            float[] embedding = parseEmbedding(response);
            if (embedding.length != dimensions) {
                throw new LlmApiException(
                        "Qianwen embedding returned " + embedding.length + " dimensions, expected "
                                + dimensions + ". Check app.embedding.qianwen.dimensions matches the "
                                + "PostgreSQL vector column (e.g. ai_helpers.embedding).");
            }
            return embedding;
        } catch (WebClientResponseException e) {
            log.error("Qianwen embedding API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmApiException("Qianwen embedding API failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            if (e instanceof LlmApiException) {
                throw (LlmApiException) e;
            }
            throw new LlmApiException("Qianwen embedding API failed: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getDimensions() {
        return dimensions;
    }

    /** {@inheritDoc} */
    @Override
    public String getModelName() {
        return model;
    }

    /** {@inheritDoc} */
    @Override
    public boolean testConnection() {
        if (apiKey.isBlank()) {
            return false;
        }
        try {
            generateEmbedding("ping");
            return true;
        } catch (Exception e) {
            log.warn("Qianwen embedding connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isCompatibleModeEndpoint() {
        return endpointUrl.contains("compatible-mode");
    }

    /**
     * 解析向量响应：兼容模式 {@code data[0].embedding}，原生 {@code output.embeddings[0].embedding}。
     *
     * @param responseJson 原始响应 JSON
     * @return 向量数组
     */
    static float[] parseEmbedding(String responseJson) {
        try {
            JsonNode root = MAPPER.readTree(responseJson);

            JsonNode compatibleVector = root.path("data").path(0).path("embedding");
            if (compatibleVector.isArray() && !compatibleVector.isEmpty()) {
                return toFloatArray(compatibleVector);
            }

            JsonNode nativeEmbeddings = root.path("output").path("embeddings");
            if (nativeEmbeddings.isArray() && !nativeEmbeddings.isEmpty()) {
                JsonNode nativeVector = nativeEmbeddings.get(0).path("embedding");
                if (nativeVector.isArray() && !nativeVector.isEmpty()) {
                    return toFloatArray(nativeVector);
                }
            }

            throw new LlmApiException(
                    "Qianwen embedding response missing data[0].embedding or output.embeddings[0].embedding");
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmApiException("Failed to parse Qianwen embedding response", e);
        }
    }

    private static float[] toFloatArray(JsonNode vectorNode) {
        List<Float> values = new ArrayList<>();
        vectorNode.forEach(n -> values.add((float) n.asDouble()));
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}
