package com.dataflow.ai.infrastructure.client.datasource;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据源基础设施门面：封装 JDBC 连接测试等跨模块共用的底层能力。
 */
@Component
public class DataSourceClientFacade {

    private static final int DEFAULT_VALID_SECONDS = 5;

    /**
     * 使用已解密的连接配置测试 JDBC 是否可达。
     *
     * @param decryptedConnectionConfig 含 url、username、password 等键的 Map
     * @return 连接有效返回 {@code true}
     */
    public boolean testJdbcConnection(Map<String, Object> decryptedConnectionConfig) {
        return JdbcConnectionTester.test(decryptedConnectionConfig, DEFAULT_VALID_SECONDS);
    }
}
