package com.dataflow.ai.infrastructure.client.embedding;

import com.dataflow.ai.infrastructure.client.llm.LlmApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * OpenAI 兼容 Embeddings API 实现
 */
@Slf4j
public class OpenAiCompatibleEmbeddingGenerator implements EmbeddingGenerator {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final int dimensions;
    private final String providerLabel;

    public OpenAiCompatibleEmbeddingGenerator(
            WebClient.Builder webClientBuilder,
            String baseUrl,
            String apiKey,
            String model,
            int dimensions,
            String providerLabel) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model;
        this.dimensions = dimensions;
        this.providerLabel = providerLabel;
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.webClient = webClientBuilder
                .baseUrl(normalizedBase)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (apiKey.isBlank()) {
            throw new LlmApiException(providerLabel + " embedding API key is not configured");
        }
        if (text == null || text.isBlank()) {
            return new float[dimensions];
        }
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("input", text);
        if (dimensions > 0) {
            body.put("dimensions", dimensions);
        }
        try {
            String response = webClient.post()
                    .uri("/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);
            return parseEmbedding(response);
        } catch (WebClientResponseException e) {
            log.error("{} embedding API error: status={}, body={}", providerLabel, e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmApiException(providerLabel + " embedding API failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            if (e instanceof LlmApiException) {
                throw (LlmApiException) e;
            }
            throw new LlmApiException(providerLabel + " embedding API failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean testConnection() {
        if (apiKey.isBlank()) {
            return false;
        }
        try {
            generateEmbedding("ping");
            return true;
        } catch (Exception e) {
            log.warn("{} embedding connection test failed: {}", providerLabel, e.getMessage());
            return false;
        }
    }

    static float[] parseEmbedding(String responseJson) {
        try {
            JsonNode root = MAPPER.readTree(responseJson);
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (!embeddingNode.isArray()) {
                throw new LlmApiException("Embedding response missing data[0].embedding array");
            }
            List<Float> values = new ArrayList<>();
            embeddingNode.forEach(n -> values.add((float) n.asDouble()));
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmApiException("Failed to parse embedding response", e);
        }
    }
}
