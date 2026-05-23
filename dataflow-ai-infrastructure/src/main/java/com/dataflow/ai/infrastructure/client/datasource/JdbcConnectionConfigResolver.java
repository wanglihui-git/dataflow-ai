package com.dataflow.ai.infrastructure.client.datasource;

import com.dataflow.ai.domain.enums.DataSourceType;

import java.util.Map;

/**
 * 从解密后的连接配置 Map 解析 JDBC URL（兼容 url 直传与 host/port/database 组合）。
 */
public final class JdbcConnectionConfigResolver {

    private JdbcConnectionConfigResolver() {
    }

    /**
     * 解析 JDBC URL。
     *
     * @param config 解密后的连接配置
     * @param type   数据源类型
     * @return JDBC URL；无法解析时返回 {@code null}
     */
    public static String resolveUrl(Map<String, Object> config, DataSourceType type) {
        if (config == null) {
            return null;
        }
        Object url = config.get("url");
        if (url != null && !String.valueOf(url).isBlank()) {
            return String.valueOf(url).trim();
        }
        String host = stringOrNull(config.get("host"));
        if (host == null || host.isBlank()) {
            return null;
        }
        int port = resolvePort(config, type);
        String database = stringOrNull(config.get("database"));
        if (type == DataSourceType.MYSQL) {
            String db = database != null ? database : "";
            return String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port, db);
        }
        if (type == DataSourceType.POSTGRES) {
            String db = database != null && !database.isBlank() ? database : "postgres";
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
        }
        return null;
    }

    private static int resolvePort(Map<String, Object> config, DataSourceType type) {
        Object portObj = config.get("port");
        if (portObj instanceof Number number) {
            return number.intValue();
        }
        if (portObj != null) {
            try {
                return Integer.parseInt(String.valueOf(portObj));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return type == DataSourceType.POSTGRES ? 5432 : 3306;
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
