package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.business.service.AIService;
import com.dataflow.ai.domain.entity.AiHelper;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.FeedbackType;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.dataflow.ai.domain.response.SearchSimilarResponse;
import com.dataflow.ai.infrastructure.client.embedding.EmbeddingClient;
import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI辅助服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    @Resource(name = "openAIClient")
    private LLMClient llmClient;

    private final EmbeddingClient embeddingClient;

    @Resource
    private AiHelperRepository aiHelperRepository;

    @Override
    public GenerateTransformsResponse generateTransforms(GenerateTransformsRequest request, User user) {
        log.info("Generating transforms for instruction: {}", request.getInstruction());

        // 构建上下文信息
        Map<String, Object> context = new HashMap<>();
        if (request.getContext() != null) {
            if (request.getContext().getSourceSchema() != null) {
                context.put("sourceSchema", request.getContext().getSourceSchema());
            }
            if (request.getContext().getTargetSchema() != null) {
                context.put("targetSchema", request.getContext().getTargetSchema());
            }
            if (request.getContext().getSampleData() != null) {
                context.put("sampleData", request.getContext().getSampleData());
            }
        }
        context.put("userId", user.getId());

        // 调用LLM生成转换节点
        String llmResponse = llmClient.generateTransforms(request.getInstruction(), context);

        // TODO: 解析LLM返回的JSON，提取转换节点
        // 这里需要根据LLM返回的格式进行解析
        List<com.dataflow.ai.domain.vo.Transform> transforms = List.of();

        // 生成指令的向量表示
        float[] embedding = embeddingClient.generateEmbedding(request.getInstruction());

        // 保存AI辅助记录
        AiHelper aiHelper = AiHelper.builder()
                .id(UUID.randomUUID().toString())
                .instruction(request.getInstruction())
                .context(context)
                .generatedNodes(transforms)
                .userFeedback(null)
                .embedding(embedding)
                .createdBy(user.getId())
                .createdAt(LocalDateTime.now())
                .build();
        aiHelperRepository.save(aiHelper);

        return GenerateTransformsResponse.builder()
                .nodes(transforms)
                .source(GenerateTransformsResponse.SourceInfo.builder()
                        .type("llm_generated")
                        .confidence(0.8)
                        .build())
                .suggestions(List.of(
                        GenerateTransformsResponse.Suggestion.builder()
                                .type("info")
                                .message("生成的节点需要人工审核")
                                .build()
                ))
                .visualization(GenerateTransformsResponse.Visualization.builder()
                        .summary("根据自然语言指令生成的数据转换流程")
                        .dataFlow("source -> transform -> sink")
                        .build())
                .metadata(GenerateTransformsResponse.Metadata.builder()
                        .processingTimeMs(100L)
                        .modelUsed("openai-gpt-4")
                        .build())
                .build();
    }

    @Override
    public SearchSimilarResponse searchSimilar(SearchSimilarRequest request) {
        log.info("Searching similar instructions for: {}", request.getInstruction());

        // 生成查询指令的向量
        float[] queryEmbedding = embeddingClient.generateEmbedding(request.getInstruction());

        // 使用向量搜索相似指令
        List<AiHelper> similarHelpers = aiHelperRepository.searchByEmbedding(
                queryEmbedding,
                request.getMinSimilarity(),
                request.getLimit()
        );

        // 计算相似度并转换为响应
        List<SearchSimilarResponse.SimilarResult> results = similarHelpers.stream()
                .map(helper -> {
                    double similarity = embeddingClient.cosineSimilarity(queryEmbedding, helper.getEmbedding());
                    return SearchSimilarResponse.SimilarResult.builder()
                            .instruction(helper.getInstruction())
                            .similarity(similarity)
                            .useCount(0) // TODO: 从数据库或统计中获取
                            .acceptanceRate(0.8) // TODO: 从数据库或统计中获取
                            .generatedNodes(helper.getGeneratedNodes())
                            .build();
                })
                .collect(Collectors.toList());

        return SearchSimilarResponse.builder()
                .results(results)
                .build();
    }

    @Override
    public void submitFeedback(FeedbackRequest request, User user) {
        log.info("Submitting feedback: action={}, aiHelperId={}", request.getAction(), request.getAiHelperId());

        // 查找AI辅助记录
        AiHelper aiHelper = aiHelperRepository.findById(request.getAiHelperId())
                .orElseThrow(() -> new RuntimeException("AI记录不存在"));

        // 设置用户反馈
        int feedbackValue = switch (request.getAction()) {
            case "accept" -> 1;
            case "reject" -> 0;
            case "modify" -> -1;
            default -> throw new IllegalArgumentException("无效的反馈类型: " + request.getAction());
        };
        aiHelper.setUserFeedback(feedbackValue);

        // 更新上下文中的用户修改（如果有）
        if (request.getModifiedNodes() != null && !request.getModifiedNodes().isEmpty()) {
            Map<String, Object> context = aiHelper.getContext();
            if (context == null) {
                context = new HashMap<>();
            }
            context.put("modifiedNodes", request.getModifiedNodes());
            context.put("modifiedBy", user.getId());
            context.put("modifiedAt", LocalDateTime.now());
            aiHelper.setContext(context);
        }

        aiHelperRepository.save(aiHelper);
    }
}
