package com.dataflow.ai.business.engine.retry;

import com.dataflow.ai.business.config.EngineProperties;
import com.dataflow.ai.domain.vo.ScheduleConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pipeline 执行重试门面。
 * <p>将 {@link ExponentialBackoffRetry} 接入 Source/Sink 等 IO 操作，并从调度配置或引擎默认值解析最大尝试次数。</p>
 */
@Component
@RequiredArgsConstructor
public class ExecutionRetryHelper {

    private final ExponentialBackoffRetry retryStrategy;
    private final EngineProperties engineProperties;

    /**
     * 在重试策略下执行可失败操作。
     *
     * @param executionId   执行 runId，写入重试上下文便于日志关联
     * @param operationName 操作名称（如 source-read、sink-write）
     * @param schedule      Pipeline 调度配置，可为 null（使用引擎默认重试次数）
     * @param operation     待执行逻辑
     * @param <T>           返回值类型
     * @return 操作成功时的结果
     * @throws Exception 重试耗尽后抛出最后一次异常或 {@link RetryExhaustedException}
     */
    public <T> T execute(String executionId, String operationName, ScheduleConfig schedule,
                         RetryStrategy.RetryableOperation<T> operation) throws Exception {
        int maxAttempts = resolveMaxAttempts(schedule);
        RetryContext context = RetryContext.builder()
                .executionId(executionId)
                .operationName(operationName)
                .retryIntervalMs(engineProperties.getRetryIntervalMs())
                .build();
        return retryStrategy.execute(operation, maxAttempts, context);
    }

    /**
     * 解析最大尝试次数：Schedule 中 retryCount 表示「重试次数」，总尝试 = retryCount + 1。
     *
     * @param schedule 调度配置，可为 null
     * @return 至少为 1 的尝试次数上限
     */
    private int resolveMaxAttempts(ScheduleConfig schedule) {
        if (schedule != null && schedule.getRetryCount() != null && schedule.getRetryCount() > 0) {
            return schedule.getRetryCount() + 1;
        }
        return Math.max(1, engineProperties.getDefaultMaxRetries());
    }
}
