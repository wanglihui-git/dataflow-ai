package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 分组处理器
 * 按指定字段对记录进行分组
 */
@Slf4j
@Component
public class GroupProcessor implements TransformProcessor {

    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing Group transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        @SuppressWarnings("unchecked")
        List<String> groupBy = (List<String>) context.getConfigValue("groupBy");
        String outputFormat = (String) context.getConfigValue("outputFormat", "list"); // list, map, array

        if (groupBy == null || groupBy.isEmpty()) {
            throw new com.dataflow.ai.business.engine.exception.TransformException(
                    "groupBy field list is required");
        }

        // 执行分组
        Map<String, List<Record>> groups = groupRecords(batch.getRecords(), groupBy);

        // 创建输出记录
        List<Record> resultRecords = new ArrayList<>();

        switch (outputFormat.toLowerCase()) {
            case "list":
                // 每个组一个记录，包含组的键和记录列表
                for (Map.Entry<String, List<Record>> entry : groups.entrySet()) {
                    Record groupRecord = new Record();
                    groupRecord.setId(UUID.randomUUID().toString());

                    // 解析组键
                    String[] keys = entry.getKey().split("\u0000");
                    for (int i = 0; i < keys.length; i++) {
                        groupRecord.set(groupBy.get(i), keys[i]);
                    }

                    // 添加记录列表
                    groupRecord.set("records", entry.getValue());
                    groupRecord.set("count", entry.getValue().size());

                    resultRecords.add(groupRecord);
                }
                break;

            case "map":
                // 将整个分组结构作为一个Map输出
                Record mapRecord = new Record();
                mapRecord.setId(UUID.randomUUID().toString());

                for (Map.Entry<String, List<Record>> entry : groups.entrySet()) {
                    mapRecord.set(entry.getKey(), entry.getValue());
                }

                mapRecord.set("groupCount", groups.size());
                mapRecord.set("totalRecords", batch.getRecords().size());

                resultRecords.add(mapRecord);
                break;

            case "array":
                // 返回数组结构的记录
                for (Map.Entry<String, List<Record>> entry : groups.entrySet()) {
                    Record arrayRecord = new Record();
                    arrayRecord.setId(UUID.randomUUID().toString());

                    // 添加组信息
                    arrayRecord.set("groupKey", entry.getKey());
                    arrayRecord.set("records", entry.getValue());
                    arrayRecord.set("count", entry.getValue().size());

                    // 添加每组的统计数据
                    addGroupStatistics(arrayRecord, entry.getValue());

                    resultRecords.add(arrayRecord);
                }
                break;

            default:
                log.warn("Unknown output format: {}, using list format", outputFormat);
                for (Map.Entry<String, List<Record>> entry : groups.entrySet()) {
                    Record groupRecord = new Record();
                    groupRecord.setId(UUID.randomUUID().toString());

                    String[] keys = entry.getKey().split("\u0000");
                    for (int i = 0; i < keys.length; i++) {
                        groupRecord.set(groupBy.get(i), keys[i]);
                    }

                    groupRecord.set("records", entry.getValue());
                    groupRecord.set("count", entry.getValue().size());

                    resultRecords.add(groupRecord);
                }
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_grouped")
                .sequenceNumber(batch.getSequenceNumber())
                .records(resultRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Group completed: nodeId={}, inputRecords={}, outputGroups={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    @Override
    public String getSupportedType() {
        return "GROUP";
    }

    /**
     * 按多个字段分组记录
     */
    private Map<String, List<Record>> groupRecords(List<Record> records, List<String> groupBy) {
        Map<String, List<Record>> groups = new LinkedHashMap<>();

        for (Record record : records) {
            // 构建组键
            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < groupBy.size(); i++) {
                if (i > 0) {
                    keyBuilder.append("\u0000"); // 使用null字符分隔
                }
                Object value = record.get(groupBy.get(i));
                keyBuilder.append(value != null ? value.toString() : "null");
            }

            String groupKey = keyBuilder.toString();
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(record);
        }

        return groups;
    }

    /**
     * 添加组的统计信息
     */
    private void addGroupStatistics(Record groupRecord, List<Record> records) {
        if (records.isEmpty()) {
            return;
        }

        // 统计记录数
        groupRecord.set("recordCount", records.size());

        // 统计所有数值字段的平均值
        Set<String> numericFields = findNumericFields(records);

        for (String field : numericFields) {
            double sum = 0;
            int count = 0;

            for (Record record : records) {
                Object value = record.get(field);
                if (value instanceof Number) {
                    sum += ((Number) value).doubleValue();
                    count++;
                }
            }

            if (count > 0) {
                groupRecord.set(field + "_avg", sum / count);
                groupRecord.set(field + "_sum", sum);
            }
        }

        // 添加第一条和最后一条记录的引用
        if (!records.isEmpty()) {
            groupRecord.set("firstRecord", records.get(0));
            groupRecord.set("lastRecord", records.get(records.size() - 1));
        }
    }

    /**
     * 查找所有数值字段
     */
    private Set<String> findNumericFields(List<Record> records) {
        Set<String> numericFields = new HashSet<>();

        for (Record record : records) {
            for (String field : record.getFieldNames()) {
                Object value = record.get(field);
                if (value instanceof Number) {
                    numericFields.add(field);
                }
            }
        }

        return numericFields;
    }
}
