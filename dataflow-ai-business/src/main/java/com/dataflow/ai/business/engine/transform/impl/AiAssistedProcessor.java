package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.business.service.AIService;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.domain.request.GenerateTransformsRequest;
import com.dataflow.ai.domain.response.GenerateTransformsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI辅助转换处理器
 * 使用LLM进行智能数据转换
 */
@Slf4j
@Component
public class AiAssistedProcessor implements TransformProcessor {

    @Resource
    private AIService aiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing AI-Assisted transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        String prompt = (String) context.getConfigValue("prompt");
        String outputField = (String) context.getConfigValue("outputField");
        String model = (String) context.getConfigValue("model", "default");
        Integer maxRetries = context.getConfigValue("maxRetries", 3);

        if (prompt == null || prompt.trim().isEmpty()) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.AI_ASSISTED, "prompt is required");
        }

        // 处理每个记录
        List<Record> processedRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            Record processedRecord = new Record();
            processedRecord.setId(record.getId());

            // 复制所有字段
            for (String field : record.getFieldNames()) {
                processedRecord.set(field, record.get(field));
            }

            // 构建AI请求
            GenerateTransformsRequest request = buildAiRequest(record, prompt, model);

            // 调用AI服务
            Object aiResult = callAiService(request, maxRetries, context);

            // 处理AI结果
            if (outputField != null && !outputField.isEmpty()) {
                processedRecord.set(outputField, aiResult);
            } else if (aiResult instanceof Map) {
                // 如果结果是一个Map，合并到记录中
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) aiResult;
                for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                    processedRecord.set(entry.getKey(), entry.getValue());
                }
            } else {
                // 将结果设置为"ai_result"字段
                processedRecord.set("ai_result", aiResult);
            }

            processedRecords.add(processedRecord);
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_ai_processed")
                .sequenceNumber(batch.getSequenceNumber())
                .records(processedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("AI-Assisted transform completed: nodeId={}, inputRecords={}, outputRecords={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    @Override
    public String getSupportedType() {
        return "AI_ASSISTED";
    }

    /**
     * 构建AI请求
     */
    private GenerateTransformsRequest buildAiRequest(Record record, String prompt, String model) {
        GenerateTransformsRequest request = new GenerateTransformsRequest();

        // 构建包含数据的提示词
        String fullPrompt = prompt + "\n\nRecord data:\n" + record.toJson();
        request.setInstruction(fullPrompt);

        // 设置上下文信息
        Map<String, Object> context = new HashMap<>();
        context.put("recordId", record.getId());
        context.put("fields", record.getFieldNames());
        request.setContext(context);

        return request;
    }

    /**
     * 调用AI服务（带重试）
     */
    private Object callAiService(GenerateTransformsRequest request, int maxRetries,
                                 TransformContext context) throws Exception {
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // 注意：这里简化了AIService的调用
                // 实际实现可能需要调整AIService接口或创建新的方法
                GenerateTransformsResponse response = aiService.generateTransforms(
                        request, com.dataflow.ai.domain.entity.User.builder().id("system").build());

                if (response != null) {
                    // 解析响应
                    if (response.getTransforms() != null && !response.getTransforms().isEmpty()) {
                        // 返回第一个转换的结果
                        return response.getTransforms().get(0);
                    } else if (response.getExplanation() != null) {
                        // 返回解释文本
                        return response.getExplanation();
                    }
                }

                return null;

            } catch (Exception e) {
                lastException = e;
                log.warn("AI service call failed (attempt {}/{}): {}",
                        i + 1, maxRetries, e.getMessage());

                if (i < maxRetries - 1) {
                    // 等待后重试
                    Thread.sleep(1000 * (i + 1));
                }
            }
        }

        // 所有重试都失败
        log.error("AI service call failed after {} retries", maxRetries, lastException);
        throw TransformException.processingFailed(
                context.getExecutionId(), context.getPipelineId(),
                context.getTransform().getNodeId(), context.getTransform().getName(),
                TransformType.AI_ASSISTED,
                "AI service call failed after " + maxRetries + " retries", lastException);
    }
}
