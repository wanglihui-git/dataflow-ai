package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.domain.entity.AiHelper;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.infrastructure.client.embedding.EmbeddingClient;
import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

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

    @Mock
    private AiHelperRepository aiHelperRepository;

    @InjectMocks
    private AIServiceImpl aiService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "llmClient", llmClient);
        ReflectionTestUtils.setField(aiService, "aiHelperRepository", aiHelperRepository);
        user = User.builder().id("user-001").role(UserRole.DEVELOPER).build();
    }

    @Test
    @DisplayName("generateTransforms - 调用 LLM 并持久化（节点解析待实现）")
    void generateTransforms_persistsHelper() {
        when(llmClient.generateTransforms(any(), any())).thenReturn("{}");
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1536]);
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = aiService.generateTransforms(
                GenerateTransformsRequest.builder().instruction("map fields").build(), user);

        assertNotNull(response);
        verify(aiHelperRepository).save(any());
    }

    @Test
    @DisplayName("searchSimilar - 向量检索")
    void searchSimilar_queriesRepository() {
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1536]);
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

    @Test
    @Disabled("待 LLM JSON 解析实现后补充：nodes 非空、metadata.modelUsed 来自配置")
    void generateTransforms_parsesLlmResponse() {
    }
}
