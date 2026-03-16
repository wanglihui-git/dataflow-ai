package com.dataflow.ai.business.engine.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 指数退避重试策略
 * 每次重试的等待时间按指数增长
 */
@Slf4j
@Component
public class ExponentialBackoffRetry implements RetryStrategy {

    @Override
    public <T> T execute(RetryableOperation<T> operation, int maxAttempts, RetryContext context) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            context.setCurrentAttempt(attempt);

            try {
                log.debug("Executing operation: operationName={}, attempt={}/{}, executionId={}",
                        context.getOperationName(), attempt, maxAttempts, context.getExecutionId());

                T result = operation.execute();

                if (attempt > 1) {
                    log.info("Operation succeeded after {} attempts: operationName={}, executionId={}",
                            attempt, context.getOperationName(), context.getExecutionId());
                }

                return result;

            } catch (Exception e) {
                lastException = e;
                context.setLastException(e);

                log.warn("Operation failed: operationName={}, attempt={}/{}, error={}",
                        context.getOperationName(), attempt, maxAttempts, e.getMessage());

                // 如果不是最后一次尝试，等待后重试
                if (attempt < maxAttempts) {
                    long retryInterval = context.calculateNextRetryInterval();
                    log.info("Retrying in {} ms: operationName={}, attempt={}/{}",
                            retryInterval, context.getOperationName(), attempt + 1, maxAttempts);

                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        // 所有重试都失败
        log.error("All retry attempts failed: operationName={}, maxAttempts={}, executionId={}",
                context.getOperationName(), maxAttempts, context.getExecutionId(), lastException);

        throw new RetryExhaustedException(
                "Operation failed after " + maxAttempts + " attempts: " + context.getOperationName(),
                lastException);
    }
}
