package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.business.repository.InstructionPatternRepository;
import com.dataflow.ai.business.service.AIService;
import com.dataflow.ai.business.util.InstructionHashUtils;
import com.dataflow.ai.business.util.VectorSimilarityUtils;
import com.dataflow.ai.domain.entity.AiHelper;
import com.dataflow.ai.domain.entity.InstructionPattern;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@link AIService} 实现：LLM 生成转换、向量相似检索、历史模式命中与用户反馈学习。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final LLMClient llmClient;
    private final EmbeddingClient embeddingClient;
    private final TransformResponseParser transformResponseParser;
    private final AiHelperRepository aiHelperRepository;
    private final InstructionPatternRepository instructionPatternRepository;

    @Value("${app.ai.historical-pattern-min-similarity:0.85}")
    private double historicalPatternMinSimilarity;

    /** {@inheritDoc} */
    @Override
    public GenerateTransformsResponse generateTransforms(GenerateTransformsRequest request, User user) {
        log.info("Generating transforms for instruction: {}", request.getInstruction());
        long startMs = System.currentTimeMillis();

        Map<String, Object> context = buildContext(request, user);
        float[] embedding = embeddingClient.generateEmbedding(request.getInstruction());

        // 向量检索历史模式，相似度达标则直接复用模板
        Optional<InstructionPattern> matchedPattern = findHistoricalPattern(embedding);
        if (matchedPattern.isPresent()) {
            return buildHistoricalResponse(request, user, matchedPattern.get(), embedding, context, startMs);
        }

        String llmResponse = llmClient.generateTransforms(request.getInstruction(), context);
        List<Transform> transforms = transformResponseParser.parse(llmResponse);

        String aiHelperId = UUID.randomUUID().toString();
        AiHelper aiHelper = AiHelper.builder()
                .id(aiHelperId)
                .instruction(request.getInstruction())
                .context(context)
                .generatedNodes(transforms)
                .pipelineId(request.getPipelineId())
                .userFeedback(null)
                .embedding(embedding)
                .createdBy(user.getId())
                .createdAt(LocalDateTime.now())
                .build();
        aiHelperRepository.save(aiHelper);

        long elapsed = System.currentTimeMillis() - startMs;
        return GenerateTransformsResponse.builder()
                .aiHelperId(aiHelperId)
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

    /** {@inheritDoc} */
    @Override
    public SearchSimilarResponse searchSimilar(SearchSimilarRequest request) {
        log.info("Searching similar instructions for: {}", request.getInstruction());

        float[] queryEmbedding = embeddingClient.generateEmbedding(request.getInstruction());
        double maxDistance = VectorSimilarityUtils.toMaxCosineDistance(request.getMinSimilarity());

        List<AiHelper> similarHelpers = aiHelperRepository.searchByEmbedding(
                queryEmbedding,
                maxDistance,
                request.getLimit()
        );

        List<SearchSimilarResponse.SimilarResult> results = similarHelpers.stream()
                .map(helper -> {
                    double similarity = embeddingClient.cosineSimilarity(queryEmbedding, helper.getEmbedding());
                    String hash = InstructionHashUtils.hash(helper.getInstruction());
                    Optional<InstructionPattern> pattern = instructionPatternRepository.findByInstructionHash(hash);
                    int useCount = pattern.map(InstructionPattern::getUseCount).orElse(0);
                    double acceptanceRate = pattern
                            .map(p -> p.getAcceptanceRate() != null ? p.getAcceptanceRate().doubleValue() : 0.0)
                            .orElse(estimateAcceptanceFromFeedback(helper));
                    return SearchSimilarResponse.SimilarResult.builder()
                            .instruction(helper.getInstruction())
                            .similarity(similarity)
                            .useCount(useCount)
                            .acceptanceRate(acceptanceRate)
                            .generatedNodes(helper.getGeneratedNodes())
                            .build();
                })
                .collect(Collectors.toList());

        return SearchSimilarResponse.builder()
                .results(results)
                .build();
    }

    /** {@inheritDoc} */
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

        if (request.getPipelineId() != null && !request.getPipelineId().isBlank()) {
            aiHelper.setPipelineId(request.getPipelineId());
        }

        List<Transform> nodesForPattern = aiHelper.getGeneratedNodes();
        if (request.getModifiedNodes() != null && !request.getModifiedNodes().isEmpty()) {
            Map<String, Object> ctx = aiHelper.getContext();
            if (ctx == null) {
                ctx = new HashMap<>();
            }
            ctx.put("modifiedNodes", request.getModifiedNodes());
            ctx.put("modifiedBy", user.getId());
            ctx.put("modifiedAt", LocalDateTime.now());
            aiHelper.setContext(ctx);
            nodesForPattern = request.getModifiedNodes();
        }

        aiHelperRepository.save(aiHelper);
        updateInstructionPattern(aiHelper, nodesForPattern, feedbackValue);
    }

    /**
     * 组装传给 LLM 的上下文（ schema、样本、用户与 Pipeline 关联）。
     */
    private Map<String, Object> buildContext(GenerateTransformsRequest request, User user) {
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
        if (request.getPipelineId() != null) {
            context.put("pipelineId", request.getPipelineId());
        }
        return context;
    }

    /**
     * 按配置的最小相似度检索最匹配的一条历史指令模式。
     *
     * @param embedding 当前指令向量
     * @return 命中模式，无则空
     */
    private Optional<InstructionPattern> findHistoricalPattern(float[] embedding) {
        List<InstructionPattern> patterns = instructionPatternRepository.searchByEmbedding(
                embedding,
                VectorSimilarityUtils.toMaxCosineDistance(historicalPatternMinSimilarity),
                1);
        return patterns.isEmpty() ? Optional.empty() : Optional.of(patterns.get(0));
    }

    /**
     * 基于历史模式构建响应，并递增模式使用次数。
     */
    private GenerateTransformsResponse buildHistoricalResponse(
            GenerateTransformsRequest request, User user, InstructionPattern pattern,
            float[] embedding, Map<String, Object> context, long startMs) {
        String aiHelperId = UUID.randomUUID().toString();
        AiHelper aiHelper = AiHelper.builder()
                .id(aiHelperId)
                .instruction(request.getInstruction())
                .context(context)
                .generatedNodes(pattern.getTransformTemplate())
                .pipelineId(request.getPipelineId())
                .userFeedback(null)
                .embedding(embedding)
                .createdBy(user.getId())
                .createdAt(LocalDateTime.now())
                .build();
        aiHelperRepository.save(aiHelper);

        pattern.setUseCount(pattern.getUseCount() + 1);
        pattern.setLastUsedAt(LocalDateTime.now());
        instructionPatternRepository.save(pattern);

        long elapsed = System.currentTimeMillis() - startMs;
        double rate = pattern.getAcceptanceRate() != null ? pattern.getAcceptanceRate().doubleValue() : 0.8;
        return GenerateTransformsResponse.builder()
                .aiHelperId(aiHelperId)
                .nodes(pattern.getTransformTemplate())
                .source(GenerateTransformsResponse.SourceInfo.builder()
                        .type("historical_pattern")
                        .confidence(rate)
                        .matchedInstruction(pattern.getInstructionText())
                        .build())
                .suggestions(List.of(
                        GenerateTransformsResponse.Suggestion.builder()
                                .type("info")
                                .message("命中历史指令模式，已跳过 LLM 调用")
                                .build()
                ))
                .visualization(GenerateTransformsResponse.Visualization.builder()
                        .summary("复用历史转换模板")
                        .dataFlow("historical_pattern -> transform -> sink")
                        .build())
                .metadata(GenerateTransformsResponse.Metadata.builder()
                        .processingTimeMs(elapsed)
                        .modelUsed("historical_pattern")
                        .build())
                .build();
    }

    /**
     * 根据用户反馈更新或创建 {@link InstructionPattern}（拒绝时降低采纳率）。
     *
     * @param aiHelper      AI 辅助记录
     * @param nodes         用于模板的节点列表
     * @param feedbackValue 1=接受，0=拒绝，-1=修改
     */
    private void updateInstructionPattern(AiHelper aiHelper, List<Transform> nodes, int feedbackValue) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        String hash = InstructionHashUtils.hash(aiHelper.getInstruction());
        Optional<InstructionPattern> existing = instructionPatternRepository.findByInstructionHash(hash);
        float[] embedding = aiHelper.getEmbedding();

        if (feedbackValue == 0) {
            existing.ifPresent(pattern -> {
                BigDecimal rate = pattern.getAcceptanceRate() != null ? pattern.getAcceptanceRate() : BigDecimal.ONE;
                BigDecimal lowered = rate.multiply(BigDecimal.valueOf(0.5)).max(BigDecimal.ZERO);
                pattern.setAcceptanceRate(lowered.setScale(2, RoundingMode.HALF_UP));
                instructionPatternRepository.save(pattern);
            });
            return;
        }

        List<Transform> template = nodes;
        if (existing.isPresent()) {
            InstructionPattern pattern = existing.get();
            int count = pattern.getUseCount() != null ? pattern.getUseCount() : 0;
            pattern.setUseCount(count + 1);
            pattern.setTransformTemplate(template);
            if (embedding != null && pattern.getAvgEmbedding() != null
                    && embedding.length == pattern.getAvgEmbedding().length) {
                pattern.setAvgEmbedding(mergeEmbedding(pattern.getAvgEmbedding(), count, embedding));
            } else if (embedding != null) {
                pattern.setAvgEmbedding(embedding);
            }
            BigDecimal rate = pattern.getAcceptanceRate() != null ? pattern.getAcceptanceRate() : BigDecimal.ZERO;
            BigDecimal updated = rate.multiply(BigDecimal.valueOf(count))
                    .add(BigDecimal.ONE)
                    .divide(BigDecimal.valueOf(count + 1), 2, RoundingMode.HALF_UP);
            pattern.setAcceptanceRate(updated);
            instructionPatternRepository.save(pattern);
        } else {
            InstructionPattern pattern = InstructionPattern.builder()
                    .instructionHash(hash)
                    .instructionText(aiHelper.getInstruction())
                    .transformTemplate(template)
                    .useCount(1)
                    .avgEmbedding(embedding)
                    .acceptanceRate(BigDecimal.ONE)
                    .createdAt(LocalDateTime.now())
                    .build();
            instructionPatternRepository.save(pattern);
        }
    }

    /** 将新向量与历史平均向量按使用次数做滑动平均。 */
    private static float[] mergeEmbedding(float[] avg, int useCount, float[] newEmb) {
        float[] result = new float[newEmb.length];
        for (int i = 0; i < newEmb.length; i++) {
            result[i] = (avg[i] * useCount + newEmb[i]) / (useCount + 1);
        }
        return result;
    }

    /** 无模式记录时，根据单条 AiHelper 的 userFeedback 估算采纳率。 */
    private static double estimateAcceptanceFromFeedback(AiHelper helper) {
        if (helper.getUserFeedback() == null) {
            return 0.0;
        }
        return switch (helper.getUserFeedback()) {
            case 1 -> 1.0;
            case -1 -> 0.7;
            case 0 -> 0.0;
            default -> 0.0;
        };
    }
}
