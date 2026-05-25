package com.dataflow.ai.business.engine.source.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SourceException;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.ConnectionTestResult;
import com.dataflow.ai.infrastructure.client.datasource.JdbcConnectionConfigResolver;
import com.dataflow.ai.infrastructure.client.datasource.JdbcConnectionTester;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据库源读取器
 * 支持MySQL和PostgreSQL数据库
 */
@Slf4j
@Component
public class DatabaseSourceReader implements SourceReader {

    @Resource
    private DataSourceRepository dataSourceRepository;

    @Resource
    private EncryptionService encryptionService;

    private static final int DEFAULT_PREVIEW_SIZE = 100;
    private static final int DEFAULT_FETCH_SIZE = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 300;

    /**
     * 从数据源读取记录并更新执行上下文中的已处理计数。
     *
     * @param sourceConfig 源配置
     * @param context      执行上下文
     * @return 读取到的记录列表
     * @throws Exception 连接、查询或解析失败时抛出
     */
    @Override
    public List<Record> read(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, ExecutionContext context)
            throws Exception {
        String dataSourceId = sourceConfig.getDataSourceId();
        DataSourceType type = sourceConfig.getType();

        log.info("Reading from database source: dataSourceId={}, type={}, tableName={}, query={}",
                dataSourceId, type, sourceConfig.getTableName(), sourceConfig.getQuery());

        // 获取数据源配置
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new SourceException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, type, "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = JdbcConnectionConfigResolver.resolveUrl(config, type);
        String username = stringOrNull(config.get("username"));
        String password = stringOrNull(config.get("password"));

        // 构建SQL查询
        String query = buildQuery(sourceConfig, type);

        log.debug("Executing query: {}", query);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            if (url == null || url.isBlank()) {
                throw new SourceException("连接配置不完整：请提供 url，或 host/port（及 database）");
            }

            // 建立数据库连接
            connection = createConnection(url, username, password);

            // 创建Statement
            statement = connection.createStatement();
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setFetchSize(DEFAULT_FETCH_SIZE);

            // 执行查询
            resultSet = statement.executeQuery(query);

            // 读取结果
            List<Record> records = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                // 检查是否已取消
                if (context.isCancelled()) {
                    log.warn("Execution cancelled while reading from database: runId={}", context.getRunId());
                    break;
                }

                Record record = new Record();
                record.setId(UUID.randomUUID().toString());

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    record.set(columnName, value);
                }

                records.add(record);

                // 更新上下文
                context.incrementRecordsProcessed(1);
            }

            log.info("Database read completed: runId={}, recordsRead={}", context.getRunId(), records.size());
            return records;

        } catch (SQLException e) {
            log.error("Error reading from database: dataSourceId={}, error={}", dataSourceId, e.getMessage(), e);
            throw SourceException.readFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, e);
        } finally {
            // 关闭资源
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 返回本读取器支持的数据源类型。
     *
     * @return 数据源类型枚举
     */
    @Override
    public DataSourceType getSupportedType() {
        return DataSourceType.MYSQL;
    }

    /**
     * 测试数据源是否可连接或文件是否可读。
     *
     * @param dataSource 数据源实体
     * @return 连接成功返回 true
     */
    @Override
    public ConnectionTestResult testConnection(DataSource dataSource) {
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        return JdbcConnectionTester.testResult(config, dataSource.getType(), 5);
    }

    /**
     * 采样预览数据源中的部分记录。
     *
     * @param sourceConfig 源配置
     * @param sampleSize   最大采样条数
     * @return 预览记录列表
     * @throws Exception 读取失败时抛出
     */
    @Override
    public List<Record> preview(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, int sampleSize)
            throws Exception {
        if (sampleSize <= 0) {
            sampleSize = DEFAULT_PREVIEW_SIZE;
        }

        // 创建临时上下文（没有实际执行ID）
        ExecutionContext tempContext = ExecutionContext.builder()
                .runId("preview_" + System.currentTimeMillis())
                .build();

        // 构建带LIMIT的查询
        com.dataflow.ai.domain.vo.SourceConfig limitedConfig = cloneConfig(sourceConfig);
        if (limitedConfig.getQuery() != null) {
            String query = limitedConfig.getQuery().trim();
            if (!query.toLowerCase().endsWith(";")) {
                query = query + " LIMIT " + sampleSize;
            }
            limitedConfig.setQuery(query);
        }

        // 读取数据
        List<Record> records = read(limitedConfig, tempContext);

        // 限制返回数量
        if (records.size() > sampleSize) {
            records = records.subList(0, sampleSize);
        }

        return records;
    }

    /**
     * 构建SQL查询
     */
    private String buildQuery(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, DataSourceType type) {
        if (sourceConfig.getQuery() != null && !sourceConfig.getQuery().trim().isEmpty()) {
            return sourceConfig.getQuery().trim();
        }

        if (sourceConfig.getTableName() != null && !sourceConfig.getTableName().trim().isEmpty()) {
            return "SELECT * FROM " + sourceConfig.getTableName();
        }

        throw new SourceException("Either query or tableName must be specified");
    }

    /**
     * 创建数据库连接
     */
    private Connection createConnection(String url, String username, String password) throws SQLException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            log.error("Failed to create database connection: url={}, error={}", url, e.getMessage());
            throw e;
        }
    }

    /**
     * 安静地关闭资源
     */
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("Error closing resource: {}", e.getMessage());
            }
        }
    }

    /**
     * 克隆配置对象
     */
    private com.dataflow.ai.domain.vo.SourceConfig cloneConfig(com.dataflow.ai.domain.vo.SourceConfig original) {
        return com.dataflow.ai.domain.vo.SourceConfig.builder()
                .dataSourceId(original.getDataSourceId())
                .type(original.getType())
                .tableName(original.getTableName())
                .query(original.getQuery())
                .params(original.getParams() != null ? new java.util.HashMap<>(original.getParams()) : null)
                .build();
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
