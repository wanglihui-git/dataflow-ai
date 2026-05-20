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
 * 通义千问 DashScope 原生文本向量 API
 * POST /api/v1/services/embeddings/text-embedding/text-embedding
 */
@Slf4j
public class QianwenEmbeddingGenerator implements EmbeddingGenerator {

    public static final String DEFAULT_ENDPOINT =
            "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String endpointUrl;
    private final String model;
    private final int dimensions;

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
        ObjectNode input = body.putObject("input");
        ArrayNode texts = input.putArray("texts");
        texts.add(text);
        if (dimensions > 0) {
            ObjectNode parameters = body.putObject("parameters");
            parameters.put("dimension", dimensions);
        }

        try {
            String response = webClient.post()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);
            return parseEmbedding(response);
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
            log.warn("Qianwen embedding connection test failed: {}", e.getMessage());
            return false;
        }
    }

    static float[] parseEmbedding(String responseJson) {
        try {
            JsonNode root = MAPPER.readTree(responseJson);
            JsonNode embeddings = root.path("output").path("embeddings");
            if (!embeddings.isArray() || embeddings.isEmpty()) {
                throw new LlmApiException("Qianwen embedding response missing output.embeddings");
            }
            JsonNode vectorNode = embeddings.get(0).path("embedding");
            if (!vectorNode.isArray()) {
                throw new LlmApiException("Qianwen embedding response missing embedding array");
            }
            List<Float> values = new ArrayList<>();
            vectorNode.forEach(n -> values.add((float) n.asDouble()));
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmApiException("Failed to parse Qianwen embedding response", e);
        }
    }
}
