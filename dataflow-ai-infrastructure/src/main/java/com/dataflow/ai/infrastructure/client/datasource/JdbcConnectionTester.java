package com.dataflow.ai.infrastructure.client.datasource;

import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.ConnectionTestResult;
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
        return testResult(url, username, password, validTimeoutSeconds).isConnected();
    }

    /**
     * 从解密后的连接配置 Map 读取连接信息并测试。
     *
     * @param decryptedConfig       连接配置
     * @param type                  数据源类型（用于解析 host/port 或 JDBC 驱动）
     * @param validTimeoutSeconds   isValid 超时秒数
     * @return 连通性测试结果（含失败原因）
     */
    public static ConnectionTestResult testResult(
            Map<String, Object> decryptedConfig, DataSourceType type, int validTimeoutSeconds) {
        if (decryptedConfig == null) {
            return ConnectionTestResult.failure("连接配置为空");
        }
        String url = JdbcConnectionConfigResolver.resolveUrl(decryptedConfig, type);
        if (url == null || url.isBlank()) {
            return ConnectionTestResult.failure("连接配置不完整：请提供 url，或 host/port（及 database）");
        }
        return testResult(
                url,
                stringOrNull(decryptedConfig.get("username")),
                stringOrNull(decryptedConfig.get("password")),
                validTimeoutSeconds);
    }

    /**
     * 从解密后的连接配置 Map 读取 url/username/password 并测试连接。
     *
     * @param decryptedConfig       连接配置
     * @param validTimeoutSeconds   isValid 超时秒数
     * @return 连接有效返回 {@code true}
     * @deprecated 请使用 {@link #testResult(Map, DataSourceType, int)} 以支持 host/port 配置与错误信息
     */
    @Deprecated
    public static boolean test(Map<String, Object> decryptedConfig, int validTimeoutSeconds) {
        return testResult(decryptedConfig, DataSourceType.MYSQL, validTimeoutSeconds).isConnected();
    }

    /**
     * 使用 JDBC URL 与凭据测试连接并返回详细结果。
     */
    public static ConnectionTestResult testResult(
            String url, String username, String password, int validTimeoutSeconds) {
        if (url == null || url.isBlank()) {
            return ConnectionTestResult.failure("JDBC URL 不能为空");
        }
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            if (connection.isValid(Math.max(1, validTimeoutSeconds))) {
                return ConnectionTestResult.success();
            }
            return ConnectionTestResult.failure("数据库连接无效或超时");
        } catch (SQLException e) {
            log.debug("JDBC connection test failed: {}", e.getMessage());
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ConnectionTestResult.failure("数据库连接失败: " + detail);
        }
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
