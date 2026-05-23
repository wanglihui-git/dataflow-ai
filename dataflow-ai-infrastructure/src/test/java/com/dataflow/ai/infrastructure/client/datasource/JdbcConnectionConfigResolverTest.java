package com.dataflow.ai.infrastructure.client.datasource;

import com.dataflow.ai.domain.enums.DataSourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcConnectionConfigResolverTest {

  @Test
  @DisplayName("host/port/database 解析 MySQL JDBC URL")
  void resolveUrl_mysqlFromHostPort() {
    String url = JdbcConnectionConfigResolver.resolveUrl(
        Map.of("host", "127.0.0.1", "port", 3306, "database", "demo"),
        DataSourceType.MYSQL);

    assertEquals(
        "jdbc:mysql://127.0.0.1:3306/demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        url);
  }

  @Test
  @DisplayName("缺少 host 时返回 null")
  void resolveUrl_missingHost_returnsNull() {
    assertNull(JdbcConnectionConfigResolver.resolveUrl(Map.of("port", 3306), DataSourceType.MYSQL));
  }

  @Test
  @DisplayName("无效 URL 返回失败原因")
  void testResult_invalidUrl_returnsMessage() {
    var result = JdbcConnectionTester.testResult(
        Map.of("host", "127.0.0.1", "port", 59999, "database", "demo", "username", "root", "password", "x"),
        DataSourceType.MYSQL,
        2);

    assertTrue(!result.isConnected());
    assertTrue(result.getMessage().startsWith("数据库连接失败:"));
  }
}
