package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.infrastructure.client.datasource.JdbcConnectionConfigResolver;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * 查找处理器
 * 从外部数据源查找并关联数据
 */
@Slf4j
@Component
public class LookupProcessor implements TransformProcessor {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    private static final int LOOKUP_CACHE_SIZE = 1000;

    // 简单的缓存实现
    private final Map<String, Map<String, Object>> lookupCache = new java.util.LinkedHashMap<>(LOOKUP_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
            return size() > LOOKUP_CACHE_SIZE;
        }
    };

    /**
     * 处理数据批次，执行本转换节点的业务逻辑。
     *
     * @param batch   输入数据批次
     * @param context 转换上下文（含节点配置与共享状态）
     * @return 处理后的数据批次
     * @throws Exception 配置无效或处理失败时抛出
     */
    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing Lookup transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取配置
        String dataSourceId = (String) context.getConfigValue("dataSourceId");
        String lookupTable = (String) context.getConfigValue("lookupTable");
        String lookupKey = (String) context.getConfigValue("lookupKey");
        String inputKey = (String) context.getConfigValue("inputKey");
        String outputFields = (String) context.getConfigValue("outputFields");

        if (dataSourceId == null || lookupTable == null || lookupKey == null || inputKey == null) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.LOOKUP, "dataSourceId, lookupTable, lookupKey, and inputKey are required");
        }

        // 解析输出字段
        List<String> outputFieldList = new ArrayList<>();
        if (outputFields != null && !outputFields.isEmpty()) {
            String[] fields = outputFields.split(",");
            for (String field : fields) {
                outputFieldList.add(field.trim());
            }
        }

        // 获取数据源连接
        Optional<DataSource> dataSourceOpt = dataSourceService.findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.LOOKUP, "Data source not found: " + dataSourceId);
        }

        DataSource dataSource = dataSourceOpt.get();
        Map<String, Object> connectionConfig = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = JdbcConnectionConfigResolver.resolveUrl(connectionConfig, dataSource.getType());
        if (url == null || url.isBlank()) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.LOOKUP, "连接配置不完整：请提供 url，或 host/port（及 database）");
        }
        String username = stringOrNull(connectionConfig.get("username"));
        String password = stringOrNull(connectionConfig.get("password"));

        // 预加载查找数据
        Map<String, Map<String, Object>> lookupData = preloadLookupData(url, username, password,
                lookupTable, lookupKey, outputFieldList);

        // 处理每个记录
        List<Record> processedRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            Record processedRecord = new Record();
            processedRecord.setId(record.getId());

            // 复制原始字段
            for (String field : record.getFieldNames()) {
                processedRecord.set(field, record.get(field));
            }

            // 查找并添加字段
            Object keyValue = record.get(inputKey);
            if (keyValue != null) {
                String cacheKey = dataSourceId + ":" + keyValue;
                Map<String, Object> lookedUpData = lookupData.get(keyValue.toString());

                if (lookedUpData != null) {
                    if (outputFieldList.isEmpty()) {
                        // 添加所有查找字段
                        for (Map.Entry<String, Object> entry : lookedUpData.entrySet()) {
                            processedRecord.set(entry.getKey(), entry.getValue());
                        }
                    } else {
                        // 添加指定的输出字段
                        for (String field : outputFieldList) {
                            if (lookedUpData.containsKey(field)) {
                                processedRecord.set(field, lookedUpData.get(field));
                            }
                        }
                    }
                }
            }

            processedRecords.add(processedRecord);
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_lookup")
                .sequenceNumber(batch.getSequenceNumber())
                .records(processedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Lookup completed: nodeId={}, inputRecords={}, outputRecords={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    /**
     * 返回本处理器支持的转换类型标识。
     *
     * @return 转换类型名称
     */
    @Override
    public String getSupportedType() {
        return "LOOKUP";
    }

    /**
     * 预加载查找数据
     */
    private Map<String, Map<String, Object>> preloadLookupData(String url, String username, String password,
                                                                 String table, String keyField, List<String> outputFields) {
        Map<String, Map<String, Object>> lookupData = new HashMap<>();
        String cacheKey = url + ":" + table;

        // 检查缓存
        if (lookupCache.containsKey(cacheKey)) {
//            return lookupCache.get(cacheKey);
            return null;
        }

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            connection = DriverManager.getConnection(url, username, password);
            statement = connection.createStatement();

            // 构建查询
            String query = "SELECT * FROM " + table;
            if (!outputFields.isEmpty()) {
                StringBuilder sb = new StringBuilder("SELECT ");
                sb.append(keyField);
                for (String field : outputFields) {
                    if (!field.equals(keyField)) {
                        sb.append(", ").append(field);
                    }
                }
                sb.append(" FROM ").append(table);
                query = sb.toString();
            }

            log.debug("Executing lookup query: {}", query);

            resultSet = statement.executeQuery(query);

            // 读取结果
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Object keyValue = resultSet.getObject(keyField);
                if (keyValue != null) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        row.put(columnName, resultSet.getObject(i));
                    }
                    lookupData.put(keyValue.toString(), row);
                }
            }

            // 缓存结果
//            lookupCache.put(cacheKey, lookupData);

            log.debug("Lookup data preloaded: table={}, rows={}", table, lookupData.size());

        } catch (SQLException e) {
            log.error("Error preloading lookup data: table={}, error={}", table, e.getMessage(), e);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }

        return lookupData;
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("Error closing resource: {}", e.getMessage());
            }
        }
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
