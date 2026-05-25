package com.dataflow.ai.business.engine.metrics;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.orchestrator.ExecutionResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行指标收集器
 * 收集Pipeline执行过程中的各种指标
 */
@Slf4j
@Component
public class ExecutionMetricsCollector {

    private final Map<String, ExecutionMetrics> metricsMap = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    /**
     * 构造指标收集器；{@link MeterRegistry} 可选，未注入时仅收集内存指标。
     *
     * @param meterRegistry Micrometer 注册表，可为 null
     */
    public ExecutionMetricsCollector(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 为一次执行创建内存指标快照并记录开始时间。
     *
     * @param context 执行上下文（使用 runId 作为键）
     */
    public void initialize(ExecutionContext context) {
        ExecutionMetrics metrics = new ExecutionMetrics();
        metrics.setRunId(context.getRunId());
        metrics.setStartTime(LocalDateTime.now());
        metrics.setStartTimeMs(System.currentTimeMillis());

        metricsMap.put(context.getRunId(), metrics);

        log.debug("Metrics initialized: runId={}", context.getRunId());
    }

    /**
     * 开始收集源数据指标
     */
    public void startSourceMetric(ExecutionContext context, String dataSourceId, String type) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null) {
            return;
        }

        ExecutionResult.DataSourceMetrics sourceMetric = ExecutionResult.DataSourceMetrics.builder()
                .dataSourceId(dataSourceId)
                .type(type)
                .role("SOURCE")
                .build();

        metrics.setCurrentSourceMetric(sourceMetric);
        metrics.setSourceStartTimeMs(System.currentTimeMillis());

        log.debug("Source metric started: runId={}, dataSourceId={}", context.getRunId(), dataSourceId);
    }

    /**
     * 结束收集源数据指标
     */
    public void endSourceMetric(ExecutionContext context, String dataSourceId, long recordsRead) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null || metrics.getCurrentSourceMetric() == null) {
            return;
        }

        ExecutionResult.DataSourceMetrics sourceMetric = metrics.getCurrentSourceMetric();
        sourceMetric.setRecordsRead(recordsRead);
        sourceMetric.setDurationMs(System.currentTimeMillis() - metrics.getSourceStartTimeMs());

        metrics.addDataSourceMetric(dataSourceId, sourceMetric);
        metrics.setCurrentSourceMetric(null);

        log.debug("Source metric ended: runId={}, dataSourceId={}, recordsRead={}, durationMs={}",
                context.getRunId(), dataSourceId, recordsRead, sourceMetric.getDurationMs());
    }

    /**
     * 开始收集转换指标
     */
    public void startTransformMetric(ExecutionContext context, String nodeId) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null) {
            return;
        }

        TransformMetric transformMetric = new TransformMetric();
        transformMetric.setNodeId(nodeId);
        transformMetric.setStartTimeMs(System.currentTimeMillis());

        metrics.setCurrentTransformMetric(transformMetric);
        metrics.getTransformMetrics().put(nodeId, transformMetric);

        log.debug("Transform metric started: runId={}, nodeId={}", context.getRunId(), nodeId);
    }

    /**
     * 结束收集转换指标
     */
    public void endTransformMetric(ExecutionContext context, String nodeId, long recordsProcessed) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null) {
            return;
        }

        TransformMetric transformMetric = metrics.getTransformMetrics().get(nodeId);
        if (transformMetric != null) {
            transformMetric.setRecordsProcessed(recordsProcessed);
            transformMetric.setDurationMs(System.currentTimeMillis() - transformMetric.getStartTimeMs());

            log.debug("Transform metric ended: runId={}, nodeId={}, recordsProcessed={}, durationMs={}",
                    context.getRunId(), nodeId, recordsProcessed, transformMetric.getDurationMs());
        }
    }

    /**
     * 开始收集目标指标
     */
    public void startSinkMetric(ExecutionContext context, String dataSourceId, String type) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null) {
            return;
        }

        ExecutionResult.DataSourceMetrics sinkMetric = ExecutionResult.DataSourceMetrics.builder()
                .dataSourceId(dataSourceId)
                .type(type)
                .role("SINK")
                .build();

        metrics.setCurrentSinkMetric(sinkMetric);
        metrics.setSinkStartTimeMs(System.currentTimeMillis());

        log.debug("Sink metric started: runId={}, dataSourceId={}", context.getRunId(), dataSourceId);
    }

    /**
     * 结束收集目标指标
     */
    public void endSinkMetric(ExecutionContext context, String dataSourceId, long recordsWritten) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null || metrics.getCurrentSinkMetric() == null) {
            return;
        }

        ExecutionResult.DataSourceMetrics sinkMetric = metrics.getCurrentSinkMetric();
        sinkMetric.setRecordsWritten(recordsWritten);
        sinkMetric.setDurationMs(System.currentTimeMillis() - metrics.getSinkStartTimeMs());

        metrics.addDataSourceMetric(dataSourceId, sinkMetric);
        metrics.setCurrentSinkMetric(null);

        log.debug("Sink metric ended: runId={}, dataSourceId={}, recordsWritten={}, durationMs={}",
                context.getRunId(), dataSourceId, recordsWritten, sinkMetric.getDurationMs());
    }

    /**
     * 汇总本次执行的时间、吞吐、转换节点与内存指标，并发布到 Micrometer。
     *
     * @param context 执行上下文
     * @return 最终指标 Map；无快照时返回空 Map
     */
    public Map<String, Object> collectFinalMetrics(ExecutionContext context) {
        ExecutionMetrics metrics = metricsMap.get(context.getRunId());
        if (metrics == null) {
            return new HashMap<>();
        }

        metrics.setEndTime(LocalDateTime.now());
        metrics.setEndTimeMs(System.currentTimeMillis());

        Map<String, Object> finalMetrics = new HashMap<>();

        // 基本信息
        finalMetrics.put("runId", context.getRunId());
        finalMetrics.put("pipelineId", context.getPipeline().getId());
        finalMetrics.put("pipelineName", context.getPipeline().getName());

        // 时间指标
        long durationMs = metrics.getEndTimeMs() - metrics.getStartTimeMs();
        finalMetrics.put("startTime", metrics.getStartTime());
        finalMetrics.put("endTime", metrics.getEndTime());
        finalMetrics.put("durationMs", durationMs);
        finalMetrics.put("durationSeconds", durationMs / 1000.0);

        // 记录指标
        finalMetrics.put("recordsProcessed", context.getRecordsProcessed().get());
        finalMetrics.put("recordsFailed", context.getRecordsFailed().get());
        finalMetrics.put("totalRecords", context.getRecordsProcessed().get() + context.getRecordsFailed().get());
        finalMetrics.put("batchesProcessed", context.getBatchesProcessed().get());

        // 成功率
        long totalRecords = finalMetrics.get("totalRecords") != null ? (long) finalMetrics.get("totalRecords") : 0;
        double successRate = totalRecords > 0
                ? (double) context.getRecordsProcessed().get() / totalRecords
                : 0.0;
        finalMetrics.put("successRate", successRate);

        // 吞吐量
        if (durationMs > 0) {
            double recordsPerSecond = (double) context.getRecordsProcessed().get() / (durationMs / 1000.0);
            finalMetrics.put("recordsPerSecond", recordsPerSecond);
        }

        // 转换节点指标
        Map<String, Map<String, Object>> transformMetricsSummary = new HashMap<>();
        for (TransformMetric tm : metrics.getTransformMetrics().values()) {
            Map<String, Object> tmSummary = new HashMap<>();
            tmSummary.put("recordsProcessed", tm.getRecordsProcessed());
            tmSummary.put("durationMs", tm.getDurationMs());
            if (tm.getDurationMs() > 0) {
                double recordsPerMs = (double) tm.getRecordsProcessed() / tm.getDurationMs();
                tmSummary.put("recordsPerSecond", recordsPerMs * 1000);
            }
            transformMetricsSummary.put(tm.getNodeId(), tmSummary);
        }
        finalMetrics.put("transformMetrics", transformMetricsSummary);

        // 内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        finalMetrics.put("memoryUsedBytes", usedMemory);
        finalMetrics.put("memoryMaxBytes", maxMemory);
        finalMetrics.put("memoryUsedMb", usedMemory / (1024.0 * 1024.0));
        finalMetrics.put("memoryMaxMb", maxMemory / (1024.0 * 1024.0));
        finalMetrics.put("memoryUsagePercent", (double) usedMemory / maxMemory * 100);

        log.debug("Final metrics collected: runId={}, totalMetrics={}", context.getRunId(), finalMetrics.size());

        publishMicrometer(context, finalMetrics);

        metricsMap.remove(context.getRunId());

        return finalMetrics;
    }

    /**
     * 将汇总指标发布到 Micrometer（计数器与计时器）。
     *
     * @param context      执行上下文
     * @param finalMetrics 已汇总的最终指标 Map
     */
    private void publishMicrometer(ExecutionContext context, Map<String, Object> finalMetrics) {
        if (meterRegistry == null) {
            return;
        }
        // 步骤1：按 pipeline_id 打标签
        String pipelineId = context.getPipeline().getId();
        Tags tags = Tags.of("pipeline_id", pipelineId);
        long records = context.getRecordsProcessed().get();
        meterRegistry.counter("dataflow.pipeline.records.processed", tags).increment(records);
        Object durationMs = finalMetrics.get("durationMs");
        if (durationMs instanceof Number n) {
            meterRegistry.timer("dataflow.pipeline.execution.duration", tags)
                    .record(java.time.Duration.ofMillis(n.longValue()));
        }
    }

    /**
     * 获取数据源指标
     */
    public Map<String, ExecutionResult.DataSourceMetrics> getDataSourceMetrics() {
        ExecutionMetrics metrics = metricsMap.values().stream()
                .filter(m -> m.getCurrentSourceMetric() != null || m.getCurrentSinkMetric() != null)
                .findFirst()
                .orElse(null);

        if (metrics == null) {
            return new HashMap<>();
        }

        return new HashMap<>(metrics.getDataSourceMetrics());
    }

    /**
     * 获取指定执行ID的指标
     */
    public ExecutionMetrics getMetrics(String runId) {
        return metricsMap.get(runId);
    }

    /**
     * 清理指定执行ID的指标
     */
    public void cleanup(String runId) {
        metricsMap.remove(runId);
        log.debug("Metrics cleaned up: runId={}", runId);
    }

    /**
     * 清理所有指标
     */
    public void cleanupAll() {
        metricsMap.clear();
        log.debug("All metrics cleaned up");
    }

    /**
     * 获取指标统计
     */
    public Map<String, Object> getMetricsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeMetricsCount", metricsMap.size());
        stats.put("totalMetricsCollected", metricsMap.size());
        return stats;
    }

    /**
     * 单次 Pipeline 执行的内存指标快照（源/汇/转换各阶段耗时与计数）。
     */
    private static class ExecutionMetrics {
        private String runId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long startTimeMs;
        private long endTimeMs;
        private ExecutionResult.DataSourceMetrics currentSourceMetric;
        private long sourceStartTimeMs;
        private ExecutionResult.DataSourceMetrics currentSinkMetric;
        private long sinkStartTimeMs;
        private final Map<String, TransformMetric> transformMetrics = new ConcurrentHashMap<>();
        private final Map<String, ExecutionResult.DataSourceMetrics> dataSourceMetrics = new ConcurrentHashMap<>();

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }

        public void setStartTimeMs(long startTimeMs) {
            this.startTimeMs = startTimeMs;
        }

        public long getEndTimeMs() {
            return endTimeMs;
        }

        public void setEndTimeMs(long endTimeMs) {
            this.endTimeMs = endTimeMs;
        }

        public ExecutionResult.DataSourceMetrics getCurrentSourceMetric() {
            return currentSourceMetric;
        }

        public void setCurrentSourceMetric(ExecutionResult.DataSourceMetrics currentSourceMetric) {
            this.currentSourceMetric = currentSourceMetric;
        }

        public long getSourceStartTimeMs() {
            return sourceStartTimeMs;
        }

        public void setSourceStartTimeMs(long sourceStartTimeMs) {
            this.sourceStartTimeMs = sourceStartTimeMs;
        }

        public ExecutionResult.DataSourceMetrics getCurrentSinkMetric() {
            return currentSinkMetric;
        }

        public void setCurrentSinkMetric(ExecutionResult.DataSourceMetrics currentSinkMetric) {
            this.currentSinkMetric = currentSinkMetric;
        }

        public long getSinkStartTimeMs() {
            return sinkStartTimeMs;
        }

        public void setSinkStartTimeMs(long sinkStartTimeMs) {
            this.sinkStartTimeMs = sinkStartTimeMs;
        }

        public Map<String, TransformMetric> getTransformMetrics() {
            return transformMetrics;
        }

        public Map<String, ExecutionResult.DataSourceMetrics> getDataSourceMetrics() {
            return dataSourceMetrics;
        }

        public void addDataSourceMetric(String dataSourceId, ExecutionResult.DataSourceMetrics metric) {
            dataSourceMetrics.put(dataSourceId, metric);
        }

        public void setCurrentTransformMetric(TransformMetric transformMetric) {
            transformMetrics.put(transformMetric.getNodeId(), transformMetric);
        }
    }

    /**
     * 单个 Transform 节点的处理耗时与记录数。
     */
    private static class TransformMetric {
        private String nodeId;
        private long recordsProcessed;
        private long startTimeMs;
        private long durationMs;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public long getRecordsProcessed() {
            return recordsProcessed;
        }

        public void setRecordsProcessed(long recordsProcessed) {
            this.recordsProcessed = recordsProcessed;
        }

        public long getStartTimeMs() {
            return startTimeMs;
        }

        public void setStartTimeMs(long startTimeMs) {
            this.startTimeMs = startTimeMs;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }
}
