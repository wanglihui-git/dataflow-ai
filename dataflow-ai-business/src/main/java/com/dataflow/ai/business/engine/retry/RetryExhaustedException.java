package com.dataflow.ai.business.engine.retry;

/**
 * 重试耗尽异常。
 * <p>当 {@link RetryStrategy} 在达到最大尝试次数后仍失败时抛出。</p>
 */
public class RetryExhaustedException extends RuntimeException {

    private final String operationName;
    private final int maxAttempts;

    /**
     * @param message 错误描述
     */
    public RetryExhaustedException(String message) {
        super(message);
        this.operationName = null;
        this.maxAttempts = 0;
    }

    /**
     * @param message 错误描述
     * @param cause   最后一次失败的根因
     */
    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
        this.operationName = null;
        this.maxAttempts = 0;
    }

    /**
     * @param operationName 操作名称
     * @param maxAttempts   已尝试次数上限
     * @param cause         最后一次异常
     */
    public RetryExhaustedException(String operationName, int maxAttempts, Throwable cause) {
        super("Retry exhausted for operation: " + operationName + " after " + maxAttempts + " attempts", cause);
        this.operationName = operationName;
        this.maxAttempts = maxAttempts;
    }

    /**
     * @return 失败的操作名称，简单构造时为 null
     */
    public String getOperationName() {
        return operationName;
    }

    /**
     * @return 配置的最大尝试次数，简单构造时为 0
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
