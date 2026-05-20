package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.domain.entity.AiHelper;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.vo.Transform;
import com.dataflow.ai.infrastructure.client.embedding.EmbeddingClient;
import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import com.dataflow.ai.infrastructure.client.llm.TransformResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceImplTest {

    @Mock
    private LLMClient llmClient;

    @Mock
    private EmbeddingClient embeddingClient;

    @Spy
    private TransformResponseParser transformResponseParser = new TransformResponseParser();

    @Mock
    private AiHelperRepository aiHelperRepository;

    @InjectMocks
    private AIServiceImpl aiService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-001").role(UserRole.DEVELOPER).build();
    }

    @Test
    @DisplayName("generateTransforms - 调用 LLM 并持久化")
    void generateTransforms_persistsHelper() {
        when(llmClient.generateTransforms(any(), any())).thenReturn("{\"nodes\":[]}");
        when(llmClient.getModelName()).thenReturn("qwen-plus");
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = aiService.generateTransforms(
                GenerateTransformsRequest.builder().instruction("map fields").build(), user);

        assertNotNull(response);
        verify(aiHelperRepository).save(any());
    }

    @Test
    @DisplayName("generateTransforms - 解析 LLM JSON 填充 nodes 与 modelUsed")
    void generateTransforms_parsesLlmResponse() {
        String llmJson = """
                {
                  "nodes": [
                    {
                      "nodeId": "n1",
                      "type": "FILTER",
                      "name": "Filter rows",
                      "dependsOn": []
                    }
                  ]
                }
                """;
        when(llmClient.generateTransforms(any(), any())).thenReturn(llmJson);
        when(llmClient.getModelName()).thenReturn("qwen-plus");
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = aiService.generateTransforms(
                GenerateTransformsRequest.builder().instruction("filter").build(), user);

        assertEquals(1, response.getNodes().size());
        assertEquals("n1", response.getNodes().get(0).getNodeId());
        assertEquals(TransformType.FILTER, response.getNodes().get(0).getType());
        assertEquals("qwen-plus", response.getMetadata().getModelUsed());
        assertNotNull(response.getMetadata().getProcessingTimeMs());
    }

    @Test
    @DisplayName("searchSimilar - 向量检索")
    void searchSimilar_queriesRepository() {
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.searchByEmbedding(any(), anyDouble(), anyInt())).thenReturn(List.of());

        aiService.searchSimilar(SearchSimilarRequest.builder().instruction("test").build());

        verify(aiHelperRepository).searchByEmbedding(any(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("submitFeedback - 更新反馈")
    void submitFeedback_updatesRecord() {
        AiHelper helper = AiHelper.builder().id("ai-1").instruction("x").build();
        when(aiHelperRepository.findById("ai-1")).thenReturn(Optional.of(helper));
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiService.submitFeedback(FeedbackRequest.builder()
                .aiHelperId("ai-1")
                .action("accept")
                .build(), user);

        verify(aiHelperRepository).save(any());
    }
}
