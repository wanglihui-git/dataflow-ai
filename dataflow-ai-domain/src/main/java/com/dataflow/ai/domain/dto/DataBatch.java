package com.dataflow.ai.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据批次
 * 用于批处理优化内存使用和性能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataBatch {

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 批次序号
     */
    private int sequenceNumber;

    /**
     * 批次中的记录
     */
    @Builder.Default
    private List<Record> records = new ArrayList<>();

    /**
     * 批次元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理开始时间
     */
    private LocalDateTime processingStartedAt;

    /**
     * 处理结束时间
     */
    private LocalDateTime processingCompletedAt;

    /**
     * 是否为最后一个批次
     */
    @Builder.Default
    private boolean lastBatch = false;

    /**
     * 添加记录到批次
     */
    public void addRecord(Record record) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }
        records.add(record);
    }

    /**
     * 添加多个记录到批次
     */
    public void addRecords(List<Record> newRecords) {
        if (newRecords == null) {
            throw new IllegalArgumentException("Records cannot be null");
        }
        records.addAll(newRecords);
    }

    /**
     * 获取批次大小
     */
    public int size() {
        return records.size();
    }

    /**
     * 检查批次是否为空
     */
    public boolean isEmpty() {
        return records.isEmpty();
    }

    /**
     * 清空批次
     */
    public void clear() {
        records.clear();
    }

    /**
     * 获取指定位置的记录
     */
    public Record getRecord(int index) {
        if (index < 0 || index >= records.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + records.size());
        }
        return records.get(index);
    }

    /**
     * 创建当前批次的切片
     */
    public DataBatch slice(int fromIndex, int toIndex) {
        DataBatch slice = new DataBatch();
        slice.setBatchId(this.batchId + "_slice");
        slice.setSequenceNumber(this.sequenceNumber);
        slice.setMetadata(new HashMap<>(this.metadata));
        slice.setLastBatch(this.lastBatch);
        slice.setRecords(new ArrayList<>(records.subList(fromIndex, toIndex)));
        return slice;
    }

    /**
     * 按字段过滤记录
     */
    public DataBatch filter(String field, Object value) {
        DataBatch filtered = new DataBatch();
        filtered.setBatchId(this.batchId + "_filtered");
        filtered.setSequenceNumber(this.sequenceNumber);
        filtered.setMetadata(new HashMap<>(this.metadata));
        filtered.setLastBatch(this.lastBatch);

        for (Record record : records) {
            Object fieldValue = record.get(field);
            if (fieldValue == null && value == null) {
                filtered.addRecord(record);
            } else if (fieldValue != null && fieldValue.equals(value)) {
                filtered.addRecord(record);
            }
        }
        return filtered;
    }

    /**
     * 选择指定字段
     */
    public DataBatch select(List<String> fields) {
        DataBatch selected = new DataBatch();
        selected.setBatchId(this.batchId + "_selected");
        selected.setSequenceNumber(this.sequenceNumber);
        selected.setMetadata(new HashMap<>(this.metadata));
        selected.setLastBatch(this.lastBatch);

        for (Record record : records) {
            Record newRecord = new Record();
            newRecord.setId(record.getId());
            for (String field : fields) {
                if (record.containsField(field)) {
                    newRecord.set(field, record.get(field));
                }
            }
            selected.addRecord(newRecord);
        }
        return selected;
    }

    /**
     * 获取批次处理时长（毫秒）
     */
    public Long getProcessingDurationMs() {
        if (processingStartedAt == null || processingCompletedAt == null) {
            return null;
        }
        return java.time.Duration.between(processingStartedAt, processingCompletedAt).toMillis();
    }

    /**
     * 开始处理批次
     */
    public void startProcessing() {
        this.processingStartedAt = LocalDateTime.now();
    }

    /**
     * 完成批次处理
     */
    public void completeProcessing() {
        this.processingCompletedAt = LocalDateTime.now();
    }

    /**
     * 分割批次为多个小批次
     */
    public List<DataBatch> split(int batchSize) {
        List<DataBatch> batches = new ArrayList<>();
        int totalRecords = records.size();
        int batchCount = (totalRecords + batchSize - 1) / batchSize;

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, totalRecords);
            DataBatch subBatch = slice(fromIndex, toIndex);
            subBatch.setSequenceNumber(i + 1);
            subBatch.setLastBatch(i == batchCount - 1);
            batches.add(subBatch);
        }
        return batches;
    }

    /**
     * 合并多个批次
     */
    public static DataBatch merge(List<DataBatch> batches) {
        DataBatch merged = new DataBatch();
        merged.setBatchId("merged_" + System.currentTimeMillis());
        merged.setCreatedAt(LocalDateTime.now());

        if (!batches.isEmpty()) {
            merged.setSequenceNumber(batches.get(0).getSequenceNumber());
            merged.setLastBatch(batches.get(batches.size() - 1).isLastBatch());

            for (DataBatch batch : batches) {
                merged.addRecords(batch.getRecords());
            }
        }

        return merged;
    }

    /**
     * 转换为Map列表
     */
    public List<Map<String, Object>> toMapList() {
        return records.stream()
                .map(Record::getData)
                .collect(Collectors.toList());
    }
}
