package com.dataflow.ai.infrastructure.client.datasource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * JdbcConnectionTester JDBC 连通性探测单测。
 */

class JdbcConnectionTesterTest {

    /**
     * 验证：无效 URL 返回 false。
     */
    @Test
    @DisplayName("无效 URL 返回 false")
    void invalidUrl_returnsFalse() {
        assertFalse(JdbcConnectionTester.test("jdbc:invalid://none", "u", "p", 2));
        assertFalse(JdbcConnectionTester.test(Map.of("url", ""), 2));
    }
}
