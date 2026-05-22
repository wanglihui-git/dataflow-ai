package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.business.repository.InstructionPatternRepository;
import com.dataflow.ai.domain.entity.AiHelper;
import com.dataflow.ai.domain.entity.InstructionPattern;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AIServiceImpl LLM 生成、向量检索与反馈单测。
 */

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

    @Mock
    private InstructionPatternRepository instructionPatternRepository;

    @InjectMocks
    private AIServiceImpl aiService;

    private User user;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "historicalPatternMinSimilarity", 0.85);
        user = User.builder().id("user-001").role(UserRole.DEVELOPER).build();
    }

    /**
     * 验证：generateTransforms - 调用 LLM 并返回 aiHelperId。
     */
    @Test
    @DisplayName("generateTransforms - 调用 LLM 并返回 aiHelperId")
    void generateTransforms_persistsHelper() {
        // 准备：配置 Mock 返回值
        when(instructionPatternRepository.searchByEmbedding(any(), anyDouble(), anyInt())).thenReturn(List.of());
        when(llmClient.generateTransforms(any(), any())).thenReturn("{\"nodes\":[]}");
        when(llmClient.getModelName()).thenReturn("qwen-plus");
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 执行：调用被测方法
        var response = aiService.generateTransforms(
                GenerateTransformsRequest.builder().instruction("map fields").build(), user);

        // 断言：校验响应或交互
        assertNotNull(response.getAiHelperId());
        verify(llmClient).generateTransforms(any(), any());
        verify(aiHelperRepository).save(any());
    }

    /**
     * 验证：generateTransforms - 命中历史模式跳过 LLM。
     */
    @Test
    @DisplayName("generateTransforms - 命中历史模式跳过 LLM")
    void generateTransforms_historicalPattern() {
        List<Transform> template = List.of(Transform.builder()
                .nodeId("n1").type(TransformType.FILTER).build());
        InstructionPattern pattern = InstructionPattern.builder()
                .instructionText("filter old")
                .transformTemplate(template)
                .acceptanceRate(BigDecimal.valueOf(0.9))
                .useCount(2)
                .build();
        // 准备：配置 Mock 返回值
        when(instructionPatternRepository.searchByEmbedding(any(), anyDouble(), eq(1)))
                .thenReturn(List.of(pattern));
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(instructionPatternRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 执行：调用被测方法
        var response = aiService.generateTransforms(
                GenerateTransformsRequest.builder().instruction("filter").build(), user);

        // 断言：校验响应或交互
        assertEquals("historical_pattern", response.getSource().getType());
        assertEquals(1, response.getNodes().size());
        verify(llmClient, never()).generateTransforms(any(), any());
    }

    /**
     * 验证：generateTransforms - 解析 LLM JSON。
     */
    @Test
    @DisplayName("generateTransforms - 解析 LLM JSON")
    void generateTransforms_parsesLlmResponse() {
        // 准备：配置 Mock 返回值
        when(instructionPatternRepository.searchByEmbedding(any(), anyDouble(), anyInt())).thenReturn(List.of());
        String llmJson = """
                {"nodes":[{"nodeId":"n1","type":"FILTER","dependsOn":[]}]}
                """;
        when(llmClient.generateTransforms(any(), any())).thenReturn(llmJson);
        when(llmClient.getModelName()).thenReturn("qwen-plus");
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 执行：调用被测方法
        var response = aiService.generateTransforms(
                GenerateTransformsRequest.builder().instruction("filter").build(), user);

        // 断言：校验响应或交互
        assertEquals(1, response.getNodes().size());
        assertEquals(TransformType.FILTER, response.getNodes().get(0).getType());
    }

    /**
     * 验证：searchSimilar - 使用距离阈值查询。
     */
    @Test
    @DisplayName("searchSimilar - 使用距离阈值查询")
    void searchSimilar_queriesRepository() {
        // 准备：配置 Mock 返回值
        when(embeddingClient.generateEmbedding(any())).thenReturn(new float[1024]);
        when(aiHelperRepository.searchByEmbedding(any(), anyDouble(), anyInt())).thenReturn(List.of());

        aiService.searchSimilar(SearchSimilarRequest.builder()
                .instruction("test")
                .minSimilarity(0.8)
                .build());

        // 断言：校验响应或交互
        verify(aiHelperRepository).searchByEmbedding(any(), org.mockito.ArgumentMatchers.doubleThat(d -> Math.abs(d - 0.2) < 0.001), eq(5));
    }

    /**
     * 验证：submitFeedback - accept 写入 instruction_patterns。
     */
    @Test
    @DisplayName("submitFeedback - accept 写入 instruction_patterns")
    void submitFeedback_acceptUpsertsPattern() {
        AiHelper helper = AiHelper.builder()
                .id("ai-1")
                .instruction("map a to b")
                .generatedNodes(List.of(Transform.builder().nodeId("n1").type(TransformType.FIELD_MAPPER).build()))
                .embedding(new float[1024])
                .build();
        // 准备：配置 Mock 返回值
        when(aiHelperRepository.findById("ai-1")).thenReturn(Optional.of(helper));
        when(aiHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(instructionPatternRepository.findByInstructionHash(any())).thenReturn(Optional.empty());
        when(instructionPatternRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiService.submitFeedback(FeedbackRequest.builder()
                .aiHelperId("ai-1")
                .action("accept")
                .pipelineId("pipe-1")
                .build(), user);

        // 断言：校验响应或交互
        verify(instructionPatternRepository).save(any(InstructionPattern.class));
        assertEquals("pipe-1", helper.getPipelineId());
    }
}
