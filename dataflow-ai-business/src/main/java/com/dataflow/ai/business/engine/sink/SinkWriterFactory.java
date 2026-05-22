package com.dataflow.ai.business.engine.sink;

import com.dataflow.ai.business.engine.exception.SinkException;
import com.dataflow.ai.business.engine.sink.impl.ApiSinkWriter;
import com.dataflow.ai.business.engine.sink.impl.CsvSinkWriter;
import com.dataflow.ai.business.engine.sink.impl.DatabaseSinkWriter;
import com.dataflow.ai.business.engine.sink.impl.KafkaSinkWriter;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 目标写入器工厂
 * 根据数据源类型创建对应的写入器
 */
@Slf4j
@Component
public class SinkWriterFactory {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    private final Map<DataSourceType, SinkWriter> writers = new HashMap<>();

    @Resource
    private DatabaseSinkWriter databaseSinkWriter;

    @Resource
    private ApiSinkWriter apiSinkWriter;

    @Resource
    private CsvSinkWriter csvSinkWriter;

    @Resource
    private KafkaSinkWriter kafkaSinkWriter;

    /**
     * 启动时注册各 {@link DataSourceType} 与对应 {@link SinkWriter} 实现。
     */
    @PostConstruct
    public void init() {
        writers.put(DataSourceType.MYSQL, databaseSinkWriter);
        writers.put(DataSourceType.POSTGRES, databaseSinkWriter);
        writers.put(DataSourceType.API, apiSinkWriter);
        writers.put(DataSourceType.CSV, csvSinkWriter);
        writers.put(DataSourceType.KAFKA, kafkaSinkWriter);

        log.info("SinkWriterFactory initialized with {} writer types", writers.size());
    }

    /**
     * 根据数据源创建写入器
     */
    public SinkWriter createWriter(DataSource dataSource) {
        if (dataSource == null) {
            throw new SinkException("DataSource cannot be null");
        }

        DataSourceType type = dataSource.getType();
        SinkWriter writer = writers.get(type);

        if (writer == null) {
            throw new SinkException("Unsupported data source type: " + type);
        }

        log.debug("Created sink writer for type: {}", type);
        return writer;
    }

    /**
     * 根据数据源ID创建写入器
     */
    public SinkWriter createWriterByDataSourceId(String dataSourceId) {
        Optional<DataSource> dataSourceOpt = dataSourceService.findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw new SinkException("Data source not found: " + dataSourceId);
        }
        return createWriter(dataSourceOpt.get());
    }

    /**
     * 根据数据源ID获取数据源
     */
    public Optional<DataSource> getDataSourceById(String dataSourceId) {
        return dataSourceService.findById(dataSourceId);
    }

    /**
     * 获取解密后的连接配置
     */
    public Map<String, Object> getDecryptedConnectionConfig(DataSource dataSource) {
        Map<String, Object> encryptedConfig = dataSource.getConnectionConfig();
        if (encryptedConfig == null) {
            return new java.util.HashMap<>();
        }
        return encryptionService.decrypt(encryptedConfig);
    }

    /**
     * 检查是否支持指定类型
     */
    public boolean isSupported(DataSourceType type) {
        return writers.containsKey(type);
    }

    /**
     * 获取所有支持的数据源类型
     */
    public java.util.Set<DataSourceType> getSupportedTypes() {
        return writers.keySet();
    }
}
