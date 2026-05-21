package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.infrastructure.client.llm.LLMClient;
import com.dataflow.ai.infrastructure.client.llm.RowTransformPromptBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI辅助转换处理器：按行调用 LLM 转换记录字段
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAssistedProcessor implements TransformProcessor {

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing AI-Assisted transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        String prompt = (String) context.getConfigValue("prompt");
        String outputField = (String) context.getConfigValue("outputField");
        Integer maxRetries = context.getConfigValue("maxRetries", 3);

        if (prompt == null || prompt.trim().isEmpty()) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.AI_ASSISTED, "prompt is required");
        }

        List<Record> processedRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            Record processedRecord = new Record();
            processedRecord.setId(record.getId());
            for (String field : record.getFieldNames()) {
                processedRecord.set(field, record.get(field));
            }

            Object aiResult = callAiForRecord(record, prompt, maxRetries, context);
            if (outputField != null && !outputField.isEmpty()) {
                processedRecord.set(outputField, aiResult);
            } else if (aiResult instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) aiResult;
                for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                    processedRecord.set(entry.getKey(), entry.getValue());
                }
            } else {
                processedRecord.set("ai_result", aiResult);
            }
            processedRecords.add(processedRecord);
        }

        return DataBatch.builder()
                .batchId(batch.getBatchId() + "_ai_processed")
                .sequenceNumber(batch.getSequenceNumber())
                .records(processedRecords)
                .metadata(new HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();
    }

    @Override
    public String getSupportedType() {
        return "AI_ASSISTED";
    }

    private Object callAiForRecord(Record record, String prompt, int maxRetries, TransformContext context)
            throws Exception {
        String userMessage = prompt + "\n\nRecord JSON:\n" + record.toJson();
        Map<String, Object> llmContext = Map.of("recordId", record.getId());

        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String response = llmClient.complete(
                        RowTransformPromptBuilder.SYSTEM_PROMPT, userMessage, llmContext);
                return parseRowTransformResponse(response);
            } catch (Exception e) {
                lastException = e;
                log.warn("AI row transform failed (attempt {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i < maxRetries - 1) {
                    Thread.sleep(1000L * (i + 1));
                }
            }
        }
        throw TransformException.processingFailed(
                context.getExecutionId(), context.getPipelineId(),
                context.getTransform().getNodeId(), context.getTransform().getName(),
                TransformType.AI_ASSISTED,
                "AI row transform failed after " + maxRetries + " retries", lastException);
    }

    private Object parseRowTransformResponse(String response) throws Exception {
        String json = response.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            if (start > 0) {
                json = json.substring(start + 1);
            }
            int end = json.lastIndexOf("```");
            if (end >= 0) {
                json = json.substring(0, end);
            }
            json = json.trim();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return json;
        }
    }
}
