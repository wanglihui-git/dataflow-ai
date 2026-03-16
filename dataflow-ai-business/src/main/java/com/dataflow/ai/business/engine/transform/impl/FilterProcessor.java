package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.enums.TransformType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 过滤处理器
 * 根据条件过滤记录
 */
@Slf4j
@Component
public class FilterProcessor implements TransformProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing Filter transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取过滤条件
        String condition = (String) context.getConfigValue("condition");
        String field = (String) context.getConfigValue("field");
        String operator = (String) context.getConfigValue("operator");
        Object value = context.getConfigValue("value");
        boolean keepMatching = context.getConfigValue("keepMatching", true);

        if (condition == null && (field == null || operator == null)) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.FILTER, "Either 'condition' or ('field' and 'operator') must be specified");
        }

        // 处理每个记录
        List<Record> filteredRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            boolean match;

            if (condition != null) {
                // 使用条件表达式过滤
                match = evaluateCondition(record, condition);
            } else {
                // 使用字段操作符过滤
                match = evaluateFieldFilter(record, field, operator, value);
            }

            if (match == keepMatching) {
                filteredRecords.add(record);
            }
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_filtered")
                .sequenceNumber(batch.getSequenceNumber())
                .records(filteredRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Filter completed: nodeId={}, inputRecords={}, outputRecords={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    @Override
    public String getSupportedType() {
        return "FILTER";
    }

    /**
     * 评估条件表达式
     * 简化版实现，支持基本的JSONPath表达式
     */
    private boolean evaluateCondition(Record record, String condition) {
        try {
            // 简单的条件评估逻辑
            // 实际实现可能需要集成表达式引擎如 SpEL 或 MVEL
            JsonNode recordNode = objectMapper.valueToTree(record.getData());
            JsonNode conditionNode = objectMapper.readTree(condition);

            // 如果条件是一个布尔值
            if (conditionNode.isBoolean()) {
                return conditionNode.asBoolean();
            }

            // 如果条件是一个字段名，返回该字段是否存在
            if (conditionNode.isTextual()) {
                String fieldName = conditionNode.asText();
                return record.containsField(fieldName) && record.get(fieldName) != null;
            }

            // 简单的相等比较
            if (conditionNode.has("field") && conditionNode.has("equals")) {
                String fieldName = conditionNode.get("field").asText();
                Object expectedValue = objectMapper.treeToValue(conditionNode.get("equals"), Object.class);
                Object actualValue = record.get(fieldName);

                if (expectedValue == null) {
                    return actualValue == null;
                }
                return expectedValue.equals(actualValue);
            }

            // 默认返回true
            return true;

        } catch (Exception e) {
            log.error("Error evaluating condition: condition={}, error={}", condition, e.getMessage());
            return false;
        }
    }

    /**
     * 评估字段过滤器
     */
    private boolean evaluateFieldFilter(Record record, String field, String operator, Object value) {
        Object fieldValue = record.get(field);

        switch (operator.toLowerCase()) {
            case "eq":
            case "=":
            case "==":
                return fieldValue != null && fieldValue.equals(value);
            case "ne":
            case "!=":
            case "<>":
                return !fieldValue.equals(value);
            case "gt":
            case ">":
                return compareValues(fieldValue, value) > 0;
            case "gte":
            case ">=":
                return compareValues(fieldValue, value) >= 0;
            case "lt":
            case "<":
                return compareValues(fieldValue, value) < 0;
            case "lte":
            case "<=":
                return compareValues(fieldValue, value) <= 0;
            case "contains":
                return fieldValue != null && fieldValue.toString().contains(value.toString());
            case "not_contains":
                return fieldValue == null || !fieldValue.toString().contains(value.toString());
            case "starts_with":
                return fieldValue != null && fieldValue.toString().startsWith(value.toString());
            case "ends_with":
                return fieldValue != null && fieldValue.toString().endsWith(value.toString());
            case "is_null":
                return fieldValue == null;
            case "is_not_null":
                return fieldValue != null;
            case "is_empty":
                return fieldValue == null || fieldValue.toString().isEmpty();
            case "is_not_empty":
                return fieldValue != null && !fieldValue.toString().isEmpty();
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * 比较两个值
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }

        // 尝试作为数字比较
        if (value1 instanceof Number && value2 instanceof Number) {
            double num1 = ((Number) value1).doubleValue();
            double num2 = ((Number) value2).doubleValue();
            return Double.compare(num1, num2);
        }

        // 作为字符串比较
        return value1.toString().compareTo(value2.toString());
    }
}
