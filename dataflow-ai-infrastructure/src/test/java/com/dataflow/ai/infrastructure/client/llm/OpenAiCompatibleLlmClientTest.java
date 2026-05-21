package com.dataflow.ai.infrastructure.client.llm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleLlmClientTest {

    private MockWebServer mockWebServer;
    private OpenAiCompatibleLlmClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1").toString().replaceAll("/$", "");
        client = new OpenAiCompatibleLlmClient(
                WebClient.builder(),
                "test-key",
                baseUrl,
                "gpt-4",
                100,
                0.1,
                "Test");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("generateTransforms - 调用 chat/completions 并提取 content")
    void generateTransforms_success() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "choices": [{
                            "message": {
                              "content": "{\\"nodes\\":[]}"
                            }
                          }]
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        String content = client.generateTransforms("map a to b", Map.of());
        assertEquals("{\"nodes\":[]}", content);
    }

    @Test
    @DisplayName("complete - 自定义 system/user 提示词")
    void complete_customPrompts() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":\"{\\\"k\\\":1}\"}}]}")
                .addHeader("Content-Type", "application/json"));

        String content = client.complete("sys", "user msg", Map.of());
        assertEquals("{\"k\":1}", content);
    }

    @Test
    @DisplayName("generateTransforms - 无 API Key 失败")
    void generateTransforms_missingApiKey() {
        OpenAiCompatibleLlmClient noKey = new OpenAiCompatibleLlmClient(
                WebClient.builder(), "", mockWebServer.url("/v1").toString(), "m", 10, 0.1, "Test");
        assertThrows(LlmApiException.class, () -> noKey.generateTransforms("x", Map.of()));
    }

    @Test
    @DisplayName("extractChatContent - 解析响应")
    void extractChatContent() {
        String json = """
                {"choices":[{"message":{"content":"hello"}}]}
                """;
        assertEquals("hello", OpenAiCompatibleLlmClient.extractChatContent(json));
    }

    @Test
    @DisplayName("testConnection - HTTP 200 返回 true")
    void testConnection_success() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"choices\":[{\"message\":{\"content\":\"pong\"}}]}")
                .addHeader("Content-Type", "application/json"));
        assertTrue(client.testConnection());
    }
}
