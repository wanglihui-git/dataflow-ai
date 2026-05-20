package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.business.service.AIService;
import com.dataflow.ai.domain.entity.AiHelper;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.request.FeedbackRequest;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.request.SearchSimilarRequest;
import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.dataflow.ai.domain.response.SearchSimilarResponse;
import com.dataflow.ai.domain.vo.Transform;
import com.dataflow.ai.infrastructure.client.embedding.EmbeddingClient;
import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import com.dataflow.ai.infrastructure.client.llm.TransformResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final LLMClient llmClient;
    private final EmbeddingClient embeddingClient;
    private final TransformResponseParser transformResponseParser;
    private final AiHelperRepository aiHelperRepository;

    @Override
    public GenerateTransformsResponse generateTransforms(GenerateTransformsRequest request, User user) {
        log.info("Generating transforms for instruction: {}", request.getInstruction());
        long startMs = System.currentTimeMillis();

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

        String llmResponse = llmClient.generateTransforms(request.getInstruction(), context);
        List<Transform> transforms = transformResponseParser.parse(llmResponse);

        float[] embedding = embeddingClient.generateEmbedding(request.getInstruction());

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

        long elapsed = System.currentTimeMillis() - startMs;

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
                        .processingTimeMs(elapsed)
                        .modelUsed(llmClient.getModelName())
                        .build())
                .build();
    }

    @Override
    public SearchSimilarResponse searchSimilar(SearchSimilarRequest request) {
        log.info("Searching similar instructions for: {}", request.getInstruction());

        float[] queryEmbedding = embeddingClient.generateEmbedding(request.getInstruction());

        List<AiHelper> similarHelpers = aiHelperRepository.searchByEmbedding(
                queryEmbedding,
                request.getMinSimilarity(),
                request.getLimit()
        );

        List<SearchSimilarResponse.SimilarResult> results = similarHelpers.stream()
                .map(helper -> {
                    double similarity = embeddingClient.cosineSimilarity(queryEmbedding, helper.getEmbedding());
                    return SearchSimilarResponse.SimilarResult.builder()
                            .instruction(helper.getInstruction())
                            .similarity(similarity)
                            .useCount(0)
                            .acceptanceRate(0.8)
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

        AiHelper aiHelper = aiHelperRepository.findById(request.getAiHelperId())
                .orElseThrow(() -> new RuntimeException("AI记录不存在"));

        int feedbackValue = switch (request.getAction()) {
            case "accept" -> 1;
            case "reject" -> 0;
            case "modify" -> -1;
            default -> throw new IllegalArgumentException("无效的反馈类型: " + request.getAction());
        };
        aiHelper.setUserFeedback(feedbackValue);

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
