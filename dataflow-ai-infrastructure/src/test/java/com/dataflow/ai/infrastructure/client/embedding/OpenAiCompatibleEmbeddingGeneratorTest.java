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

class OpenAiCompatibleEmbeddingGeneratorTest {

    private MockWebServer mockWebServer;
    private OpenAiCompatibleEmbeddingGenerator generator;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/v1").toString().replaceAll("/$", "");
        generator = new OpenAiCompatibleEmbeddingGenerator(
                WebClient.builder(), baseUrl, "test-key", "text-embedding-test", 3, "Test");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

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
        assertEquals(3, vec.length);
        assertEquals(0.1f, vec[0], 0.001f);
    }

    @Test
    @DisplayName("generateEmbedding - 无 API Key 失败")
    void generateEmbedding_missingApiKey() {
        OpenAiCompatibleEmbeddingGenerator noKey = new OpenAiCompatibleEmbeddingGenerator(
                WebClient.builder(), mockWebServer.url("/v1").toString(), "", "m", 3, "Test");
        assertThrows(LlmApiException.class, () -> noKey.generateEmbedding("x"));
    }

    @Test
    @DisplayName("parseEmbedding - 静态解析")
    void parseEmbedding() {
        String json = "{\"data\":[{\"embedding\":[1.0,2.0]}]}";
        float[] vec = OpenAiCompatibleEmbeddingGenerator.parseEmbedding(json);
        assertEquals(2, vec.length);
        assertEquals(1.0f, vec[0]);
    }

    @Test
    @DisplayName("getDimensions")
    void getDimensions() {
        assertEquals(3, generator.getDimensions());
    }
}
