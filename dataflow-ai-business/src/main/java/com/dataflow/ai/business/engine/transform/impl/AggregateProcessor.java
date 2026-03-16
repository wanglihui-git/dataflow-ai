package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 聚合处理器
 * 对数据进行聚合计算（sum, avg, min, max, count等）
 */
@Slf4j
@Component
public class AggregateProcessor implements TransformProcessor {

    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing Aggregate transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggregations = (List<Map<String, Object>>) context.getConfigValue("aggregations");
        String groupBy = (String) context.getConfigValue("groupBy");

        if (aggregations == null || aggregations.isEmpty()) {
            throw new com.dataflow.ai.business.engine.exception.TransformException(
                    "aggregations configuration is required");
        }

        Map<String, List<Record>> groups;

        // 分组处理
        if (groupBy != null && !groupBy.isEmpty()) {
            // 按指定字段分组
            groups = groupByField(batch.getRecords(), groupBy);
        } else {
            // 所有记录作为一组
            groups = new HashMap<>();
            groups.put("all", batch.getRecords());
        }

        // 计算聚合结果
        List<Record> resultRecords = new ArrayList<>();
        for (Map.Entry<String, List<Record>> entry : groups.entrySet()) {
            Record aggregatedRecord = new Record();
            aggregatedRecord.setId(UUID.randomUUID().toString());

            // 设置分组键
            if (groupBy != null && !groupBy.isEmpty()) {
                aggregatedRecord.set(groupBy, entry.getKey());
            }

            // 计算每个聚合操作
            for (Map<String, Object> aggConfig : aggregations) {
                String operation = (String) aggConfig.get("operation");
                String field = (String) aggConfig.get("field");
                String outputField = (String) aggConfig.get("outputField");

                if (outputField == null || outputField.isEmpty()) {
                    outputField = field + "_" + operation;
                }

                Object result = calculateAggregation(entry.getValue(), operation, field);
                aggregatedRecord.set(outputField, result);
            }

            resultRecords.add(aggregatedRecord);
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_aggregated")
                .sequenceNumber(batch.getSequenceNumber())
                .records(resultRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Aggregate completed: nodeId={}, inputRecords={}, outputGroups={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    @Override
    public String getSupportedType() {
        return "AGGREGATE";
    }

    /**
     * 按字段分组
     */
    private Map<String, List<Record>> groupByField(List<Record> records, String field) {
        Map<String, List<Record>> groups = new LinkedHashMap<>();

        for (Record record : records) {
            Object value = record.get(field);
            String key = value != null ? value.toString() : "null";

            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        return groups;
    }

    /**
     * 计算聚合操作
     */
    private Object calculateAggregation(List<Record> records, String operation, String field) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        switch (operation.toLowerCase()) {
            case "count":
                return records.size();

            case "sum":
                return calculateSum(records, field);

            case "avg":
                return calculateAvg(records, field);

            case "min":
                return calculateMin(records, field);

            case "max":
                return calculateMax(records, field);

            case "first":
                return records.get(0).get(field);

            case "last":
                return records.get(records.size() - 1).get(field);

            case "concat":
                return calculateConcat(records, field);

            case "distinct_count":
                return calculateDistinctCount(records, field);

            default:
                log.warn("Unknown aggregation operation: {}", operation);
                return null;
        }
    }

    /**
     * 计算总和
     */
    private double calculateSum(List<Record> records, String field) {
        double sum = 0;
        for (Record record : records) {
            Object value = record.get(field);
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
            }
        }
        return sum;
    }

    /**
     * 计算平均值
     */
    private double calculateAvg(List<Record> records, String field) {
        double sum = 0;
        int count = 0;

        for (Record record : records) {
            Object value = record.get(field);
            if (value instanceof Number) {
                sum += ((Number) value).doubleValue();
                count++;
            }
        }

        return count > 0 ? sum / count : 0;
    }

    /**
     * 计算最小值
     */
    private Object calculateMin(List<Record> records, String field) {
        Object min = null;

        for (Record record : records) {
            Object value = record.get(field);
            if (value != null && value instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable = (Comparable<Object>) value;
                if (min == null || comparable.compareTo(min) < 0) {
                    min = value;
                }
            }
        }

        return min;
    }

    /**
     * 计算最大值
     */
    private Object calculateMax(List<Record> records, String field) {
        Object max = null;

        for (Record record : records) {
            Object value = record.get(field);
            if (value != null && value instanceof Comparable) {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable = (Comparable<Object>) value;
                if (max == null || comparable.compareTo(max) > 0) {
                    max = value;
                }
            }
        }

        return max;
    }

    /**
     * 连接字符串
     */
    private String calculateConcat(List<Record> records, String field) {
        StringBuilder sb = new StringBuilder();

        for (Record record : records) {
            Object value = record.get(field);
            if (value != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(value.toString());
            }
        }

        return sb.toString();
    }

    /**
     * 计算去重后的数量
     */
    private int calculateDistinctCount(List<Record> records, String field) {
        Set<Object> distinctValues = new HashSet<>();

        for (Record record : records) {
            Object value = record.get(field);
            distinctValues.add(value);
        }

        return distinctValues.size();
    }
}
