package com.dataflow.ai.business.engine.retry;

/**
 * 重试耗尽异常
 * 当所有重试尝试都失败时抛出
 */
public class RetryExhaustedException extends RuntimeException {

    private final String operationName;
    private final int maxAttempts;

    public RetryExhaustedException(String message) {
        super(message);
        this.operationName = null;
        this.maxAttempts = 0;
    }

    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
        this.operationName = null;
        this.maxAttempts = 0;
    }

    public RetryExhaustedException(String operationName, int maxAttempts, Throwable cause) {
        super("Retry exhausted for operation: " + operationName + " after " + maxAttempts + " attempts", cause);
        this.operationName = operationName;
        this.maxAttempts = maxAttempts;
    }

    public String getOperationName() {
        return operationName;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
