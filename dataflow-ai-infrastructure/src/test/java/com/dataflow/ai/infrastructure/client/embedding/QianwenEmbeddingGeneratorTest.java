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

/**
 * QianwenEmbeddingGenerator MockWebServer 单测。
 */

class QianwenEmbeddingGeneratorTest {

    private MockWebServer mockWebServer;
    private QianwenEmbeddingGenerator generator;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String endpoint = mockWebServer.url("/text-embedding").toString();
        generator = new QianwenEmbeddingGenerator(
                WebClient.builder(), "test-key", endpoint, "text-embedding-v3", 3);
    }

    /**
     * 测试方法 tearDown。
     */
    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    /**
     * 验证：generateEmbedding - 解析 output.embeddings。
     */
    @Test
    @DisplayName("generateEmbedding - 解析 output.embeddings")
    void generateEmbedding_success() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("""
                        {
                          "output": {
                            "embeddings": [{
                              "embedding": [0.1, 0.2, 0.3]
                            }]
                          }
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
        QianwenEmbeddingGenerator noKey = new QianwenEmbeddingGenerator(
                WebClient.builder(), "", mockWebServer.url("/").toString(), "m", 3);
        // 断言：校验响应或交互
        assertThrows(LlmApiException.class, () -> noKey.generateEmbedding("x"));
    }
}
