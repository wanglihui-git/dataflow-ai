package com.dataflow.ai.infrastructure.client.llm;

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
import java.util.Map;

/**
 * 通义千问 DashScope 原生文本生成 API
 * POST /api/v1/services/aigc/text-generation/generation
 */
@Slf4j
public class QianwenLlmClient implements LLMClient {

    public static final String DEFAULT_ENDPOINT =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String endpointUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public QianwenLlmClient(
            WebClient.Builder webClientBuilder,
            String apiKey,
            String endpointUrl,
            String model,
            int maxTokens,
            double temperature) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.endpointUrl = endpointUrl == null || endpointUrl.isBlank() ? DEFAULT_ENDPOINT : endpointUrl.trim();
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generateTransforms(String prompt, Map<String, Object> context) {
        if (apiKey.isBlank()) {
            throw new LlmApiException("Qianwen API key is not configured");
        }
        ObjectNode body = buildRequestBody(
                LlmPromptBuilder.SYSTEM_PROMPT,
                LlmPromptBuilder.buildUserMessage(prompt, context));
        return postAndExtractContent(body);
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
            ObjectNode body = buildRequestBody(null, "ping");
            postAndExtractContent(body);
            return true;
        } catch (Exception e) {
            log.warn("Qianwen connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private ObjectNode buildRequestBody(String systemContent, String userContent) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);

        ObjectNode input = body.putObject("input");
        ArrayNode messages = input.putArray("messages");
        if (systemContent != null && !systemContent.isBlank()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemContent);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userContent);

        ObjectNode parameters = body.putObject("parameters");
        parameters.put("result_format", "message");
        parameters.put("max_tokens", maxTokens);
        parameters.put("temperature", temperature);
        return body;
    }

    private String postAndExtractContent(ObjectNode body) {
        try {
            String response = webClient.post()
                    .uri(endpointUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);
            return extractContent(response);
        } catch (WebClientResponseException e) {
            log.error("Qianwen API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmApiException("Qianwen API failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            if (e instanceof LlmApiException) {
                throw (LlmApiException) e;
            }
            throw new LlmApiException("Qianwen API failed: " + e.getMessage(), e);
        }
    }

    /**
     * DashScope result_format=message：output.choices[0].message.content
     */
    static String extractContent(String responseJson) {
        try {
            JsonNode root = MAPPER.readTree(responseJson);
            JsonNode content = root.path("output").path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && !content.isNull()) {
                return content.asText().trim();
            }
            JsonNode text = root.path("output").path("text");
            if (!text.isMissingNode() && !text.isNull()) {
                return text.asText().trim();
            }
            throw new LlmApiException("Qianwen response missing output.choices[0].message.content");
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmApiException("Failed to parse Qianwen response", e);
        }
    }
}
