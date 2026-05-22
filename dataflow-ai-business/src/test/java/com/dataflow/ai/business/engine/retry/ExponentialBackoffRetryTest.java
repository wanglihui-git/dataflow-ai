package com.dataflow.ai.business.engine.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ExponentialBackoffRetry 重试成功与耗尽场景。
 */

class ExponentialBackoffRetryTest {

    private final ExponentialBackoffRetry retry = new ExponentialBackoffRetry();

    /**
     * 验证：第二次成功。
     */
    @Test
    @DisplayName("第二次成功")
    void succeedsOnSecondAttempt() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        RetryContext ctx = RetryContext.builder()
                .executionId("run-1")
                .operationName("test")
                .retryIntervalMs(1)
                .build();

        String result = retry.execute((RetryStrategy.RetryableOperation<String>) () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("fail");
            }
            return "ok";
        }, 3, ctx);

        // 断言：校验响应或交互
        assertEquals("ok", result);
        assertEquals(2, attempts.get());
    }

    /**
     * 验证：耗尽重试抛 RetryExhaustedException。
     */
    @Test
    @DisplayName("耗尽重试抛 RetryExhaustedException")
    void exhaustsRetries() {
        RetryContext ctx = RetryContext.builder()
                .executionId("run-1")
                .operationName("test")
                .retryIntervalMs(1)
                .build();

        // 断言：校验响应或交互
        assertThrows(RetryExhaustedException.class, () -> retry.execute((RetryStrategy.RetryableOperation<String>) () -> {
            throw new RuntimeException("always fail");
        }, 2, ctx));
    }
}
