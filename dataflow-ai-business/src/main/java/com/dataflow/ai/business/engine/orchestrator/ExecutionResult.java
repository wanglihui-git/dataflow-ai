package com.dataflow.ai.business.engine.orchestrator;

import com.dataflow.ai.domain.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行结果
 * 封装Pipeline执行的结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 异常信息
     */
    private Throwable exception;

    /**
     * 执行开始时间
     */
    private LocalDateTime startTime;

    /**
     * 执行结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长（毫秒）
     */
    private Long durationMs;

    /**
     * 已处理记录数
     */
    private long recordsProcessed;

    /**
     * 失败记录数
     */
    private long recordsFailed;

    /**
     * 处理的批次数
     */
    private long batchesProcessed;

    /**
     * 指标数据
     */
    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    /**
     * 执行日志
     */
    @Builder.Default
    private List<String> logs = new java.util.ArrayList<>();

    /**
     * 节点级指标
     */
    @Builder.Default
    private Map<String, NodeMetrics> nodeMetrics = new HashMap<>();

    /**
     * 数据源指标
     */
    @Builder.Default
    private Map<String, DataSourceMetrics> dataSourceMetrics = new HashMap<>();

    /**
     * 创建成功结果
     */
    public static ExecutionResult success() {
        return ExecutionResult.builder()
                .status(ExecutionStatus.SUCCESS)
                .success(true)
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ExecutionResult failure(String errorMessage) {
        return ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .success(false)
                .errorMessage(errorMessage)
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败结果（带异常）
     */
    public static ExecutionResult failure(String errorMessage, Throwable exception) {
        return ExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .success(false)
                .errorMessage(errorMessage)
                .exception(exception)
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建取消结果
     */
    public static ExecutionResult cancelled() {
        return ExecutionResult.builder()
                .status(ExecutionStatus.CANCELLED)
                .success(false)
                .errorMessage("Execution was cancelled")
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 添加日志
     */
    public void addLog(String log) {
        this.logs.add(log);
    }

    /**
     * 添加指标
     */
    public void addMetric(String key, Object value) {
        this.metrics.put(key, value);
    }

    /**
     * 添加节点指标
     */
    public void addNodeMetrics(String nodeId, NodeMetrics nodeMetric) {
        this.nodeMetrics.put(nodeId, nodeMetric);
    }

    /**
     * 添加数据源指标
     */
    public void addDataSourceMetrics(String dataSourceId, DataSourceMetrics dataSourceMetric) {
        this.dataSourceMetrics.put(dataSourceId, dataSourceMetric);
    }

    /**
     * 计算并设置执行时长
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * 获取总记录数
     */
    public long getTotalRecords() {
        return recordsProcessed + recordsFailed;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = getTotalRecords();
        return total > 0 ? (double) recordsProcessed / total : 0.0;
    }

    /**
     * 构建完整的指标Map
     */
    public Map<String, Object> buildFullMetrics() {
        Map<String, Object> fullMetrics = new HashMap<>(this.metrics);
        fullMetrics.put("status", status.name());
        fullMetrics.put("success", success);
        fullMetrics.put("recordsProcessed", recordsProcessed);
        fullMetrics.put("recordsFailed", recordsFailed);
        fullMetrics.put("totalRecords", getTotalRecords());
        fullMetrics.put("batchesProcessed", batchesProcessed);
        fullMetrics.put("successRate", getSuccessRate());
        if (durationMs != null) {
            fullMetrics.put("durationMs", durationMs);
        }
        if (nodeMetrics != null && !nodeMetrics.isEmpty()) {
            fullMetrics.put("nodeMetrics", nodeMetrics);
        }
        if (dataSourceMetrics != null && !dataSourceMetrics.isEmpty()) {
            fullMetrics.put("dataSourceMetrics", dataSourceMetrics);
        }
        return fullMetrics;
    }

    /**
     * 节点指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeMetrics {
        private String nodeId;
        private String nodeName;
        private long recordsProcessed;
        private long recordsFailed;
        private long durationMs;
        private String error;
    }

    /**
     * 数据源指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceMetrics {
        private String dataSourceId;
        private String type;
        private String role;  // SOURCE or SINK
        private long recordsRead;
        private long recordsWritten;
        private long durationMs;
        private String error;
    }
}
