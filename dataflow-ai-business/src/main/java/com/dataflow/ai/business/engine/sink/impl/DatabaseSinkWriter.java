package com.dataflow.ai.business.engine.sink.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SinkException;
import com.dataflow.ai.business.engine.sink.SinkWriter;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * 数据库目标写入器
 * 支持MySQL和PostgreSQL数据库
 */
@Slf4j
@Component
public class DatabaseSinkWriter implements SinkWriter {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    private static final int QUERY_TIMEOUT_SECONDS = 300;

    /**
     * 将数据批次写入目标存储。
     *
     * @param batch      待写入批次
     * @param sinkConfig 目标配置
     * @param context    执行上下文
     * @return 实际写入的记录数
     * @throws Exception 连接或写入失败时抛出
     */
    @Override
    public long write(DataBatch batch, SinkConfig sinkConfig, ExecutionContext context) throws Exception {
        String dataSourceId = sinkConfig.getDataSourceId();
        String tableName = sinkConfig.getTableName();
        SinkConfig.WriteMode writeMode = sinkConfig.getWriteMode();

        if (tableName == null || tableName.trim().isEmpty()) {
            throw SinkException.tableNotFound(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.POSTGRES, tableName);
        }

        log.info("Writing to database sink: dataSourceId={}, tableName={}, writeMode={}, batchSize={}",
                dataSourceId, tableName, writeMode, batch.size());

        // 获取数据源配置
        DataSource dataSource = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new SinkException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, DataSourceType.POSTGRES, tableName, writeMode,
                        "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        // 步骤2：按写入模式分支（追加 / 覆盖 / 忽略重复 / 更新已有）
        long writtenCount = 0;

        switch (writeMode) {
            case APPEND:
                writtenCount = writeAppend(batch, tableName, url, username, password, context);
                break;

            case OVERWRITE:
                writtenCount = writeOverwrite(batch, tableName, url, username, password, context);
                break;

            case IGNORE_DUPLICATES:
                writtenCount = writeIgnoreDuplicates(batch, tableName, url, username, password, context);
                break;

            case UPDATE_EXISTING:
                writtenCount = writeUpdateExisting(batch, tableName, url, username, password, context);
                break;

            default:
                throw SinkException.unsupportedWriteMode(context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, DataSourceType.POSTGRES, writeMode);
        }

        log.info("Database write completed: runId={}, recordsWritten={}", context.getRunId(), writtenCount);
        return writtenCount;
    }

    /**
     * 返回本写入器支持的目标类型标识。
     *
     * @return 类型名称字符串
     */
    @Override
    public String getSupportedType() {
        return "DATABASE";
    }

    /**
     * 测试目标数据源是否可连接。
     *
     * @param dataSource 数据源实体
     * @return 连接成功返回 true
     */
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

    /**
     * 追加写入模式
     */
    private long writeAppend(DataBatch batch, String tableName, String url, String username,
                             String password, ExecutionContext context) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = createConnection(url, username, password);
            connection.setAutoCommit(false);

            // 构建INSERT语句
            String insertSql = buildInsertSql(tableName, batch.getRecords().get(0));
            statement = connection.prepareStatement(insertSql);
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

            int writtenCount = 0;
            for (Record record : batch.getRecords()) {
                setPreparedStatementParameters(statement, record);
                statement.addBatch();
                writtenCount++;
            }

            statement.executeBatch();
            connection.commit();

            log.debug("Append write completed: tableName={}, recordsWritten={}", tableName, writtenCount);
            return writtenCount;

        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }
            log.error("Error writing to database (append mode): tableName={}, error={}", tableName, e.getMessage());
            throw e;
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 覆盖写入模式
     */
    private long writeOverwrite(DataBatch batch, String tableName, String url, String username,
                                String password, ExecutionContext context) throws SQLException {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = createConnection(url, username, password);
            connection.setAutoCommit(false);

            // 先删除现有数据
            String deleteSql = "DELETE FROM " + tableName;
            statement = connection.createStatement();
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            int deletedCount = statement.executeUpdate(deleteSql);
            log.debug("Deleted {} existing records from table: {}", deletedCount, tableName);

            // 插入新数据
            String insertSql = buildInsertSql(tableName, batch.getRecords().get(0));
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                insertStatement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

                int writtenCount = 0;
                for (Record record : batch.getRecords()) {
                    setPreparedStatementParameters(insertStatement, record);
                    insertStatement.addBatch();
                    writtenCount++;
                }

                insertStatement.executeBatch();
                connection.commit();

                log.debug("Overwrite write completed: tableName={}, recordsWritten={}", tableName, writtenCount);
                return writtenCount;
            }

        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }
            log.error("Error writing to database (overwrite mode): tableName={}, error={}", tableName, e.getMessage());
            throw e;
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 忽略重复写入模式
     */
    private long writeIgnoreDuplicates(DataBatch batch, String tableName, String url, String username,
                                       String password, ExecutionContext context) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = createConnection(url, username, password);
            connection.setAutoCommit(false);

            // 构建INSERT IGNORE语句
            String insertSql = buildInsertIgnoreSql(tableName, batch.getRecords().get(0));
            statement = connection.prepareStatement(insertSql);
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

            int writtenCount = 0;
            for (Record record : batch.getRecords()) {
                setPreparedStatementParameters(statement, record);
                statement.addBatch();
                writtenCount++;
            }

            int[] batchResult = statement.executeBatch();
            connection.commit();

            // 统计实际写入的记录数（成功返回1，忽略返回0）
            long actualWritten = 0;
            for (int result : batchResult) {
                actualWritten += (result == Statement.EXECUTE_FAILED ? 0 : result);
            }

            log.debug("Ignore duplicates write completed: tableName={}, attempted={}, written={}",
                    tableName, writtenCount, actualWritten);
            return actualWritten;

        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }
            log.error("Error writing to database (ignore duplicates mode): tableName={}, error={}",
                    tableName, e.getMessage());
            throw e;
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 更新现有记录模式
     */
    private long writeUpdateExisting(DataBatch batch, String tableName, String url, String username,
                                     String password, ExecutionContext context) throws SQLException {
        Connection connection = null;

        try {
            connection = createConnection(url, username, password);
            connection.setAutoCommit(false);

            int writtenCount = 0;

            for (Record record : batch.getRecords()) {
                // 构建MERGE/UPSERT语句
                String mergeSql = buildMergeSql(tableName, record);

                try (PreparedStatement statement = connection.prepareStatement(mergeSql)) {
                    statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    setPreparedStatementParameters(statement, record);

                    int result = statement.executeUpdate();
                    if (result > 0) {
                        writtenCount++;
                    }
                }
            }

            connection.commit();

            log.debug("Update existing write completed: tableName={}, recordsUpdated={}", tableName, writtenCount);
            return writtenCount;

        } catch (SQLException e) {
            if (connection != null) {
                connection.rollback();
            }
            log.error("Error writing to database (update existing mode): tableName={}, error={}",
                    tableName, e.getMessage());
            throw e;
        } finally {
            closeQuietly(connection);
        }
    }

    /**
     * 构建INSERT语句
     */
    private String buildInsertSql(String tableName, Record record) {
        List<String> fields = new ArrayList<>(record.getFieldNames());
        Collections.sort(fields);

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");

        // 字段名
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(fields.get(i));
        }

        sql.append(") VALUES (");

        // 占位符
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }

        sql.append(")");

        return sql.toString();
    }

    /**
     * 构建INSERT IGNORE语句
     */
    private String buildInsertIgnoreSql(String tableName, Record record) {
        String insertSql = buildInsertSql(tableName, record);
        return insertSql.replace("INSERT INTO ", "INSERT IGNORE INTO ");
    }

    /**
     * 构建MERGE/UPSERT语句
     */
    private String buildMergeSql(String tableName, Record record) {
        List<String> fields = new ArrayList<>(record.getFieldNames());
        Collections.sort(fields);

        // 使用第一条字段作为主键（简化版）
        String primaryKey = fields.isEmpty() ? "id" : fields.get(0);

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");

        // 字段名
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(fields.get(i));
        }

        sql.append(") VALUES (");

        // 占位符
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }

        sql.append(") ON CONFLICT (").append(primaryKey).append(") DO UPDATE SET ");

        // 更新子句
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(fields.get(i)).append(" = EXCLUDED.").append(fields.get(i));
        }

        return sql.toString();
    }

    /**
     * 设置PreparedStatement参数
     */
    private void setPreparedStatementParameters(PreparedStatement statement, Record record) throws SQLException {
        List<String> fields = new ArrayList<>(record.getFieldNames());
        Collections.sort(fields);

        for (int i = 0; i < fields.size(); i++) {
            Object value = record.get(fields.get(i));
            statement.setObject(i + 1, value);
        }
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
}
