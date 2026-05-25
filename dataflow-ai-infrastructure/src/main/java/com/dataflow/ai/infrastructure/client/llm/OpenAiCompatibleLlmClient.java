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
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Chat Completions API 客户端实现。
 * <p>适用于 OpenAI、智谱等提供 {@code POST /chat/completions} 的厂商；通过 {@code providerLabel} 区分日志与异常文案。
 */
@Slf4j
public class OpenAiCompatibleLlmClient implements LLMClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(120);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final String providerLabel;

    /**
     * @param webClientBuilder WebClient 构建器
     * @param apiKey           API 密钥
     * @param baseUrl          API 根地址（不含尾部斜杠）
     * @param model            模型名
     * @param maxTokens        最大生成 token 数
     * @param temperature      采样温度
     * @param providerLabel    厂商显示名（日志/异常用）
     */
    public OpenAiCompatibleLlmClient(
            WebClient.Builder webClientBuilder,
            String apiKey,
            String baseUrl,
            String model,
            int maxTokens,
            double temperature,
            String providerLabel) {
        this.apiKey = apiKey == null ? "" : apiKey;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.providerLabel = providerLabel;
        this.webClient = webClientBuilder
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** {@inheritDoc} 使用 {@link LlmPromptBuilder} 默认系统与用户提示词。 */
    @Override
    public String generateTransforms(String prompt, Map<String, Object> context) {
        return complete(LlmPromptBuilder.SYSTEM_PROMPT, LlmPromptBuilder.buildUserMessage(prompt, context), context);
    }

    /** {@inheritDoc} 组装 messages 数组并调用 {@code /chat/completions}。 */
    @Override
    public String complete(String systemPrompt, String userPrompt, Map<String, Object> context) {
        if (apiKey.isBlank()) {
            throw new LlmApiException(providerLabel + " API key is not configured");
        }
        // 构建 OpenAI 风格请求体：model、messages、max_tokens、temperature
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        ArrayNode messages = body.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        try {
            // 同步阻塞调用，超时 120 秒
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);
            return extractChatContent(response);
        } catch (WebClientResponseException e) {
            log.error("{} chat API error: status={}, body={}", providerLabel, e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmApiException(providerLabel + " chat API failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            if (e instanceof LlmApiException) {
                throw (LlmApiException) e;
            }
            throw new LlmApiException(providerLabel + " chat API failed: " + e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getModelName() {
        return model;
    }

    /** {@inheritDoc} 发送极简 user 消息探测连通性。 */
    @Override
    public boolean testConnection() {
        if (apiKey.isBlank()) {
            return false;
        }
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 16);
            ArrayNode messages = body.putArray("messages");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", "ping");
            webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
            return true;
        } catch (Exception e) {
            log.warn("{} connection test failed: {}", providerLabel, e.getMessage());
            return false;
        }
    }

    /**
     * 从 Chat Completions 响应 JSON 中提取 {@code choices[0].message.content}。
     *
     * @param responseJson 原始响应体
     * @return 模型回复正文（trim 后）
     */
    static String extractChatContent(String responseJson) {
        try {
            JsonNode root = MAPPER.readTree(responseJson);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new LlmApiException("LLM response missing choices[0].message.content");
            }
            return content.asText().trim();
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmApiException("Failed to parse LLM chat response", e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
