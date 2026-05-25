package com.dataflow.ai.infrastructure.client.embedding;

import com.dataflow.ai.infrastructure.client.llm.LlmApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAiCompatibleEmbeddingGenerator MockWebServer 单测。
 */

class OpenAiCompatibleEmbeddingGeneratorTest {

    private MockWebServer mockWebServer;
    private OpenAiCompatibleEmbeddingGenerator generator;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1").toString().replaceAll("/$", "");
        generator = new OpenAiCompatibleEmbeddingGenerator(
                WebClient.builder(), baseUrl, "test-key", "text-embedding-test", 3, "Test");
    }

    /**
     * 测试方法 tearDown。
     */
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * 验证：generateEmbedding - 解析向量。
     */
    @Test
    @DisplayName("generateEmbedding - 解析向量")
    void generateEmbedding_success() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "data": [{
                            "embedding": [0.1, 0.2, 0.3]
                          }]
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        float[] vec = generator.generateEmbedding("hello");
        // 断言：校验响应或交互
        assertEquals(3, vec.length);
        assertEquals(0.1f, vec[0], 0.001f);
    }

    /**
     * 验证：generateEmbedding - 无 API Key 失败。
     */
    @Test
    @DisplayName("generateEmbedding - 无 API Key 失败")
    void generateEmbedding_missingApiKey() {
        OpenAiCompatibleEmbeddingGenerator noKey = new OpenAiCompatibleEmbeddingGenerator(
                WebClient.builder(), mockWebServer.url("/v1").toString(), "", "m", 3, "Test");
        // 断言：校验响应或交互
        assertThrows(LlmApiException.class, () -> noKey.generateEmbedding("x"));
    }

    /**
     * 验证：parseEmbedding - 静态解析。
     */
    @Test
    @DisplayName("parseEmbedding - 静态解析")
    void parseEmbedding() {
        String json = "{\"data\":[{\"embedding\":[1.0,2.0]}]}";
        float[] vec = OpenAiCompatibleEmbeddingGenerator.parseEmbedding(json);
        // 断言：校验响应或交互
        assertEquals(2, vec.length);
        assertEquals(1.0f, vec[0]);
    }

    /**
     * 验证：getDimensions。
     */
    @Test
    @DisplayName("getDimensions")
    void getDimensions() {
        // 断言：校验响应或交互
        assertEquals(3, generator.getDimensions());
    }
}
