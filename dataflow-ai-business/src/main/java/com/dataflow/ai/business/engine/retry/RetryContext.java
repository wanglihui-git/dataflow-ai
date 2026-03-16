package com.dataflow.ai.business.engine.retry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重试上下文
 * 提供重试过程中的状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryContext {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 操作名称
     */
    private String operationName;

    /**
     * 当前尝试次数
     */
    @Builder.Default
    private int currentAttempt = 0;

    /**
     * 最后一次异常
     */
    private Exception lastException;

    /**
     * 重试间隔（毫秒）
     */
    @Builder.Default
    private long retryIntervalMs = 1000;

    /**
     * 指数退避因子
     */
    @Builder.Default
    private double backoffFactor = 2.0;

    /**
     * 最大重试间隔（毫秒）
     */
    @Builder.Default
    private long maxRetryIntervalMs = 30000;

    /**
     * 是否启用指数退避
     */
    @Builder.Default
    private boolean exponentialBackoff = true;

    /**
     * 计算下一次重试的等待时间
     */
    public long calculateNextRetryInterval() {
        if (!exponentialBackoff) {
            return retryIntervalMs;
        }

        long interval = (long) (retryIntervalMs * Math.pow(backoffFactor, currentAttempt - 1));
        return Math.min(interval, maxRetryIntervalMs);
    }

    /**
     * 增加尝试次数
     */
    public void incrementAttempt() {
        this.currentAttempt++;
    }

    /**
     * 创建子上下文
     */
    public RetryContext copy() {
        return RetryContext.builder()
                .executionId(this.executionId)
                .operationName(this.operationName)
                .currentAttempt(this.currentAttempt)
                .lastException(this.lastException)
                .retryIntervalMs(this.retryIntervalMs)
                .backoffFactor(this.backoffFactor)
                .maxRetryIntervalMs(this.maxRetryIntervalMs)
                .exponentialBackoff(this.exponentialBackoff)
                .build();
    }
}
