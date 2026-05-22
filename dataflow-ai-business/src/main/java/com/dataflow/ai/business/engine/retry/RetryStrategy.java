package com.dataflow.ai.business.engine.retry;

/**
 * 重试策略接口
 * 定义操作失败时的重试行为
 */
@FunctionalInterface
public interface RetryStrategy {

    /**
     * 执行带重试的操作
     *
     * @param operation   要执行的操作
     * @param maxAttempts 最大尝试次数
     * @param context     重试上下文
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一次异常
     */
    <T> T execute(RetryableOperation<T> operation, int maxAttempts, RetryContext context) throws Exception;

    /**
     * 可重试的单次操作。
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface RetryableOperation<T> {
        /**
         * @return 操作结果
         * @throws Exception 失败时抛出，由策略决定是否重试
         */
        T execute() throws Exception;
    }
}
