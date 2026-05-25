package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 连接处理器
 * 将两个数据集连接在一起
 */
@Slf4j
@Component
public class JoinProcessor implements TransformProcessor {

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
        log.debug("Processing Join transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        String leftKey = (String) context.getConfigValue("leftKey");
        String rightKey = (String) context.getConfigValue("rightKey");
        String joinType = (String) context.getConfigValue("joinType", "inner");

        // 从共享状态获取右侧数据
        @SuppressWarnings("unchecked")
        List<Record> rightRecords = (List<Record>) context.getSharedState().get("join_right_records");

        if (rightRecords == null) {
            throw new com.dataflow.ai.business.engine.exception.TransformException(
                    "Right records not found in shared state. Join transform requires right data to be available.");
        }

        // 构建右侧数据的查找索引
        Map<Object, List<Record>> rightIndex = buildIndex(rightRecords, rightKey);

        // 执行连接
        List<Record> joinedRecords = new ArrayList<>();
        for (Record leftRecord : batch.getRecords()) {
            Object keyValue = leftRecord.get(leftKey);

            switch (joinType.toLowerCase()) {
                case "inner":
                    // 内连接：只在两边都有匹配时才保留
                    if (keyValue != null && rightIndex.containsKey(keyValue)) {
                        for (Record rightRecord : rightIndex.get(keyValue)) {
                            joinedRecords.add(joinRecords(leftRecord, rightRecord, leftKey, rightKey));
                        }
                    }
                    break;

                case "left":
                    // 左连接：保留左侧所有记录
                    if (keyValue != null && rightIndex.containsKey(keyValue)) {
                        for (Record rightRecord : rightIndex.get(keyValue)) {
                            joinedRecords.add(joinRecords(leftRecord, rightRecord, leftKey, rightKey));
                        }
                    } else {
                        // 右侧无匹配，左侧记录单独保留
                        joinedRecords.add(leftRecord);
                    }
                    break;

                case "right":
                    // 右连接：保留右侧所有记录
                    if (keyValue != null && rightIndex.containsKey(keyValue)) {
                        for (Record rightRecord : rightIndex.get(keyValue)) {
                            joinedRecords.add(joinRecords(leftRecord, rightRecord, leftKey, rightKey));
                        }
                    }
                    break;

                case "full":
                    // 全连接：保留所有记录
                    if (keyValue != null && rightIndex.containsKey(keyValue)) {
                        for (Record rightRecord : rightIndex.get(keyValue)) {
                            joinedRecords.add(joinRecords(leftRecord, rightRecord, leftKey, rightKey));
                        }
                    } else {
                        joinedRecords.add(leftRecord);
                    }
                    break;

                default:
                    log.warn("Unknown join type: {}, using inner join", joinType);
                    if (keyValue != null && rightIndex.containsKey(keyValue)) {
                        for (Record rightRecord : rightIndex.get(keyValue)) {
                            joinedRecords.add(joinRecords(leftRecord, rightRecord, leftKey, rightKey));
                        }
                    }
            }
        }

        // 对于右连接和全连接，添加右侧未匹配的记录
        if (joinType.equalsIgnoreCase("right") || joinType.equalsIgnoreCase("full")) {
            Set<Object> matchedKeys = new HashSet<>();
            for (Record leftRecord : batch.getRecords()) {
                Object keyValue = leftRecord.get(leftKey);
                if (keyValue != null) {
                    matchedKeys.add(keyValue);
                }
            }

            for (Map.Entry<Object, List<Record>> entry : rightIndex.entrySet()) {
                if (!matchedKeys.contains(entry.getKey())) {
                    for (Record rightRecord : entry.getValue()) {
                        // 创建空的左侧记录
                        Record emptyLeft = new Record();
                        emptyLeft.setId("empty_" + UUID.randomUUID().toString());
                        joinedRecords.add(joinRecords(emptyLeft, rightRecord, leftKey, rightKey));
                    }
                }
            }
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_joined")
                .sequenceNumber(batch.getSequenceNumber())
                .records(joinedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Join completed: nodeId={}, inputRecords={}, rightRecords={}, outputRecords={}",
                context.getTransform().getNodeId(), batch.size(), rightRecords.size(), result.size());

        return result;
    }

    /**
     * 返回本处理器支持的转换类型标识。
     *
     * @return 转换类型名称
     */
    @Override
    public String getSupportedType() {
        return "JOIN";
    }

    /**
     * 构建右侧数据的索引
     */
    private Map<Object, List<Record>> buildIndex(List<Record> records, String keyField) {
        Map<Object, List<Record>> index = new HashMap<>();

        for (Record record : records) {
            Object key = record.get(keyField);
            if (key != null) {
                index.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
            }
        }

        return index;
    }

    /**
     * 连接两条记录
     */
    private Record joinRecords(Record left, Record right, String leftKey, String rightKey) {
        Record joined = new Record();
        joined.setId(UUID.randomUUID().toString());

        // 添加左侧记录的字段
        for (String field : left.getFieldNames()) {
            joined.set("left_" + field, left.get(field));
        }

        // 添加右侧记录的字段
        for (String field : right.getFieldNames()) {
            joined.set("right_" + field, right.get(field));
        }

        // 合并元数据
        joined.getMetadata().putAll(left.getMetadata());
        joined.getMetadata().putAll(right.getMetadata());

        return joined;
    }
}
