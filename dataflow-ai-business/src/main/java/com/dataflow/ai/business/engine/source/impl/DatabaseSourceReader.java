package com.dataflow.ai.business.engine.source.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SourceException;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
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
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    private static final int DEFAULT_PREVIEW_SIZE = 100;
    private static final int DEFAULT_FETCH_SIZE = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 300;

    @Override
    public List<Record> read(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, ExecutionContext context)
            throws Exception {
        String dataSourceId = sourceConfig.getDataSourceId();
        DataSourceType type = sourceConfig.getType();

        log.info("Reading from database source: dataSourceId={}, type={}, tableName={}, query={}",
                dataSourceId, type, sourceConfig.getTableName(), sourceConfig.getQuery());

        // 获取数据源配置
        DataSource dataSource = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new SourceException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, type, "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        // 构建SQL查询
        String query = buildQuery(sourceConfig, type);

        log.debug("Executing query: {}", query);

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
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

    @Override
    public DataSourceType getSupportedType() {
        return DataSourceType.MYSQL;
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        Connection connection = null;
        try {
            connection = createConnection(url, username, password);
            return connection != null && connection.isValid(5);
        } catch (Exception e) {
            log.error("Database connection test failed: url={}, error={}", url, e.getMessage());
            return false;
        } finally {
            closeQuietly(connection);
        }
    }

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
}
