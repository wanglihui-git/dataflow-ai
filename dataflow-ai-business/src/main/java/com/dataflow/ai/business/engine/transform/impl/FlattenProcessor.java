package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.enums.TransformType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 扁平化处理器
 * 将嵌套的JSON结构扁平化为单层结构
 */
@Slf4j
@Component
public class FlattenProcessor implements TransformProcessor {

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
        log.debug("Processing Flatten transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        String delimiter = context.getConfigValue("delimiter", ".");
        String fieldsToFlatten = (String) context.getConfigValue("fields");

        // 处理每个记录
        List<Record> processedRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            Record flattenedRecord = flattenRecord(record, delimiter, fieldsToFlatten);
            processedRecords.add(flattenedRecord);
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_flattened")
                .sequenceNumber(batch.getSequenceNumber())
                .records(processedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Flatten completed: nodeId={}, inputRecords={}, outputRecords={}",
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
        return "FLATTEN";
    }

    /**
     * 扁平化记录
     */
    private Record flattenRecord(Record record, String delimiter, String fieldsToFlatten) {
        Record flattened = new Record();
        flattened.setId(record.getId());

        // 复制非嵌套字段
        for (String field : record.getFieldNames()) {
            Object value = record.get(field);
            if (!(value instanceof Map) && !(value instanceof List)) {
                flattened.set(field, value);
            }
        }

        // 扁平化嵌套字段
        for (String field : record.getFieldNames()) {
            Object value = record.get(field);
            if (fieldsToFlatten == null || fieldsToFlatten.contains(field)) {
                flattenField(flattened, field, value, delimiter);
            }
        }

        // 复制元数据
        flattened.getMetadata().putAll(record.getMetadata());

        return flattened;
    }

    /**
     * 递归扁平化字段
     */
    @SuppressWarnings("unchecked")
    private void flattenField(Record record, String prefix, Object value, String delimiter) {
        if (value == null) {
            record.set(prefix, null);
            return;
        }

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String newPrefix = prefix + delimiter + entry.getKey();
                flattenField(record, newPrefix, entry.getValue(), delimiter);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                String newPrefix = prefix + delimiter + i;
                flattenField(record, newPrefix, list.get(i), delimiter);
            }
        } else {
            record.set(prefix, value);
        }
    }
}
