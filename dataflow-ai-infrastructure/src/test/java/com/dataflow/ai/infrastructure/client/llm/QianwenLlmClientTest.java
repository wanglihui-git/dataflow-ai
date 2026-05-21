package com.dataflow.ai.infrastructure.client.llm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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

class QianwenLlmClientTest {

    private MockWebServer mockWebServer;
    private QianwenLlmClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String endpoint = mockWebServer.url("/generation").toString();
        client = new QianwenLlmClient(
                WebClient.builder(), "test-key", endpoint, "qwen-plus", 100, 0.1);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("generateTransforms - 原生 DashScope 请求体与响应解析")
    void generateTransforms_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "output": {
                            "choices": [{
                              "message": {
                                "content": "{\\"nodes\\":[]}"
                              }
                            }]
                          }
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        String content = client.generateTransforms("map a to b", Map.of());
        assertEquals("{\"nodes\":[]}", content);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"input\""));
        assertTrue(body.contains("\"messages\""));
        assertTrue(body.contains("\"result_format\":\"message\""));
        assertTrue(!body.contains("/chat/completions"));
    }

    @Test
    @DisplayName("extractContent - output.choices[0].message.content")
    void extractContent_messageFormat() {
        String json = """
                {"output":{"choices":[{"message":{"content":"hello"}}]}}
                """;
        assertEquals("hello", QianwenLlmClient.extractContent(json));
    }

    @Test
    @DisplayName("generateTransforms - 无 API Key 失败")
    void generateTransforms_missingApiKey() {
        QianwenLlmClient noKey = new QianwenLlmClient(
                WebClient.builder(), "", mockWebServer.url("/").toString(), "m", 10, 0.1);
        assertThrows(LlmApiException.class, () -> noKey.generateTransforms("x", Map.of()));
    }
}
