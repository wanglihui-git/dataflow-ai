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

    /**
     * 处理数据批次，执行本转换节点的业务逻辑。
     *
     * @param batch   输入数据批次
     * @param context 转换上下文（含节点配置与共享状态）
     * @return 处理后的数据批次
     * @throws Exception 配置无效或处理失败时抛出
     */
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

    /**
     * 返回本处理器支持的转换类型标识。
     *
     * @return 转换类型名称
     */
    @Override
    public String getSupportedType() {
        return "AI_ASSISTED";
    }

    private Object callAiForRecord(Record record, String prompt, int maxRetries, TransformContext context)
            throws Exception {
        // 步骤1：拼装用户消息（提示词 + 行 JSON）
        String userMessage = prompt + "\n\nRecord JSON:\n" + record.toJson();
        Map<String, Object> llmContext = Map.of("recordId", record.getId());

        Exception lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 步骤2：调用 LLM 并解析为 Map 或原始字符串
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
        // 步骤1：去除 Markdown 代码块包裹
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
        // 步骤2：优先解析为 JSON 对象，失败则退回原始文本
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return json;
        }
    }
}
