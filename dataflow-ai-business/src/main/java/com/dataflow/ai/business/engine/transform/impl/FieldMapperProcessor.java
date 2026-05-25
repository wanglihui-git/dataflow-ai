package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.enums.TransformType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字段映射处理器
 * 将源字段映射到目标字段
 */
@Slf4j
@Component
public class FieldMapperProcessor implements TransformProcessor {

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
        log.debug("Processing FieldMapper transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取映射配置
        @SuppressWarnings("unchecked")
        Map<String, String> fieldMapping = (Map<String, String>) context.getConfigValue("fieldMapping");
        boolean dropUnmapped = context.getConfigValue("dropUnmapped", false);
        boolean overwriteExisting = context.getConfigValue("overwriteExisting", true);

        if (fieldMapping == null || fieldMapping.isEmpty()) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.FIELD_MAPPER, "fieldMapping is required");
        }

        // 处理每个记录
        List<Record> processedRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            Record mappedRecord = new Record();
            mappedRecord.setId(record.getId());

            // 处理字段映射
            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String sourceField = entry.getKey();
                String targetField = entry.getValue();

                if (record.containsField(sourceField)) {
                    Object value = record.get(sourceField);
                    mappedRecord.set(targetField, value);
                } else {
                    log.debug("Source field not found: nodeId={}, field={}",
                            context.getTransform().getNodeId(), sourceField);
                }
            }

            // 如果不删除未映射字段，则复制所有未映射字段
            if (!dropUnmapped) {
                for (String field : record.getFieldNames()) {
                    if (!fieldMapping.containsKey(field) || !overwriteExisting) {
                        mappedRecord.set(field, record.get(field));
                    }
                }
            }

            // 复制元数据
            mappedRecord.getMetadata().putAll(record.getMetadata());

            processedRecords.add(mappedRecord);
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_mapped")
                .sequenceNumber(batch.getSequenceNumber())
                .records(processedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("FieldMapper completed: nodeId={}, inputRecords={}, outputRecords={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    /**
     * 返回本处理器支持的转换类型标识。
     *
     * @return 转换类型名称
     */
    @Override
    public String getSupportedType() {
        return "FIELD_MAPPER";
    }
}
