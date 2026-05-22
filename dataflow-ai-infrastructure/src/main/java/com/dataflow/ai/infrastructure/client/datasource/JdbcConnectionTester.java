package com.dataflow.ai.infrastructure.client.datasource;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * JDBC 连接探测工具类，供 {@link DataSourceClientFacade}、引擎 Source 等复用。
 */
@Slf4j
public final class JdbcConnectionTester {

    private JdbcConnectionTester() {
    }

    /**
     * 使用 JDBC URL 与凭据建立连接并调用 {@link Connection#isValid(int)}。
     *
     * @param url                  JDBC URL
     * @param username             用户名，可为 null
     * @param password             密码，可为 null
     * @param validTimeoutSeconds  isValid 超时秒数
     * @return 连接成功且有效返回 {@code true}
     */
    public static boolean test(String url, String username, String password, int validTimeoutSeconds) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return connection.isValid(Math.max(1, validTimeoutSeconds));
        } catch (SQLException e) {
            log.debug("JDBC connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从解密后的连接配置 Map 读取 url/username/password 并测试连接。
     *
     * @param decryptedConfig       连接配置
     * @param validTimeoutSeconds   isValid 超时秒数
     * @return 连接有效返回 {@code true}
     */
    public static boolean test(Map<String, Object> decryptedConfig, int validTimeoutSeconds) {
        if (decryptedConfig == null) {
            return false;
        }
        Object url = decryptedConfig.get("url");
        if (url == null) {
            return false;
        }
        return test(
                String.valueOf(url),
                stringOrNull(decryptedConfig.get("username")),
                stringOrNull(decryptedConfig.get("password")),
                validTimeoutSeconds);
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
