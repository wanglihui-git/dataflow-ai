package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 排序处理器
 * 对记录进行排序
 */
@Slf4j
@Component
public class SortProcessor implements TransformProcessor {

    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing Sort transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        String sortBy = (String) context.getConfigValue("sortBy");
        String direction = (String) context.getConfigValue("direction", "asc");

        if (sortBy == null || sortBy.isEmpty()) {
            throw new com.dataflow.ai.business.engine.exception.TransformException(
                    "sortBy field is required");
        }

        // 复制记录列表
        List<Record> sortedRecords = new ArrayList<>(batch.getRecords());

        // 排序
        sortedRecords.sort((r1, r2) -> {
            Object v1 = r1.get(sortBy);
            Object v2 = r2.get(sortBy);

            int result = compareValues(v1, v2);

            // 根据方向调整结果
            if ("desc".equalsIgnoreCase(direction)) {
                result = -result;
            }

            return result;
        });

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_sorted")
                .sequenceNumber(batch.getSequenceNumber())
                .records(sortedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Sort completed: nodeId={}, records={}, sortBy={}, direction={}",
                context.getTransform().getNodeId(), result.size(), sortBy, direction);

        return result;
    }

    @Override
    public String getSupportedType() {
        return "SORT";
    }

    /**
     * 比较两个值
     */
    @SuppressWarnings("unchecked")
    private int compareValues(Object v1, Object v2) {
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return -1;
        }
        if (v2 == null) {
            return 1;
        }

        // 尝试作为Comparable比较
        if (v1 instanceof Comparable && v2 instanceof Comparable) {
            try {
                return ((Comparable<Object>) v1).compareTo(v2);
            } catch (ClassCastException e) {
                // 类型不匹配，转换为字符串比较
            }
        }

        // 作为字符串比较
        return v1.toString().compareTo(v2.toString());
    }
}
