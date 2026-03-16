package com.dataflow.ai.business.engine.orchestrator;

import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 执行上下文
 * 存储Pipeline执行过程中的所有状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    /**
     * 执行记录ID
     */
    private String runId;

    /**
     * Pipeline对象
     */
    private Pipeline pipeline;

    /**
     * 执行记录实体（用于状态更新）
     */
    private ExecutionRun executionRun;

    /**
     * 共享状态（在Source、Transform、Sink之间共享）
     */
    @Builder.Default
    private Map<String, Object> sharedState = new ConcurrentHashMap<>();

    /**
     * 已处理记录数
     */
    @Builder.Default
    private AtomicLong recordsProcessed = new AtomicLong(0);

    /**
     * 失败记录数
     */
    @Builder.Default
    private AtomicLong recordsFailed = new AtomicLong(0);

    /**
     * 已处理批次数
     */
    @Builder.Default
    private AtomicLong batchesProcessed = new AtomicLong(0);

    /**
     * 是否已取消
     */
    @Builder.Default
    private boolean cancelled = false;

    /**
     * 开始时间
     */
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 当前阶段
     */
    @Builder.Default
    private ExecutionPhase currentPhase = ExecutionPhase.INIT;

    /**
     * 执行阶段
     */
    public enum ExecutionPhase {
        /**
         * 初始化阶段
         */
        INIT,

        /**
         * 读取源数据阶段
         */
        SOURCE,

        /**
         * 转换阶段
         */
        TRANSFORM,

        /**
         * 写入目标阶段
         */
        SINK,

        /**
         * 完成阶段
         */
        COMPLETED,

        /**
         * 失败阶段
         */
        FAILED
    }

    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 标记为已取消
     */
    public void markCancelled() {
        this.cancelled = true;
    }

    /**
     * 设置执行阶段
     */
    public void setPhase(ExecutionPhase phase) {
        this.currentPhase = phase;
    }

    /**
     * 增加已处理记录数
     */
    public long incrementRecordsProcessed(long count) {
        return recordsProcessed.addAndGet(count);
    }

    /**
     * 增加失败记录数
     */
    public long incrementRecordsFailed(long count) {
        return recordsFailed.addAndGet(count);
    }

    /**
     * 增加已处理批次数
     */
    public long incrementBatchesProcessed() {
        return batchesProcessed.incrementAndGet();
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = recordsProcessed.get() + recordsFailed.get();
        return total > 0 ? (double) recordsProcessed.get() / total : 0.0;
    }

    /**
     * 获取执行时长（毫秒）
     */
    public long getDurationMs() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }

    /**
     * 获取共享状态值
     */
    public Object getSharedState(String key) {
        return sharedState.get(key);
    }

    /**
     * 设置共享状态值
     */
    public void setSharedState(String key, Object value) {
        sharedState.put(key, value);
    }

    /**
     * 移除共享状态值
     */
    public void removeSharedState(String key) {
        sharedState.remove(key);
    }

    /**
     * 检查共享状态是否存在
     */
    public boolean containsSharedState(String key) {
        return sharedState.containsKey(key);
    }

    /**
     * 清空共享状态
     */
    public void clearSharedState() {
        sharedState.clear();
    }

    /**
     * 获取所有共享状态
     */
    public Map<String, Object> getAllSharedState() {
        return new ConcurrentHashMap<>(sharedState);
    }

    /**
     * 标记执行完成
     */
    public void markCompleted() {
        this.endTime = LocalDateTime.now();
        this.currentPhase = ExecutionPhase.COMPLETED;
    }

    /**
     * 标记执行失败
     */
    public void markFailed() {
        this.endTime = LocalDateTime.now();
        this.currentPhase = ExecutionPhase.FAILED;
    }

    /**
     * 复制上下文（用于创建子任务）
     */
    public ExecutionContext copy() {
        ExecutionContext copy = new ExecutionContext();
        copy.setRunId(this.runId);
        copy.setPipeline(this.pipeline);
        copy.setExecutionRun(this.executionRun);
        copy.setSharedState(new ConcurrentHashMap<>(this.sharedState));
        copy.setRecordsProcessed(new AtomicLong(this.recordsProcessed.get()));
        copy.setRecordsFailed(new AtomicLong(this.recordsFailed.get()));
        copy.setBatchesProcessed(new AtomicLong(this.batchesProcessed.get()));
        copy.setCancelled(this.cancelled);
        copy.setStartTime(this.startTime);
        copy.setEndTime(this.endTime);
        copy.setCurrentPhase(this.currentPhase);
        return copy;
    }
}
