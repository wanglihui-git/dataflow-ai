package com.dataflow.ai.business.engine.source;

import com.dataflow.ai.business.engine.exception.SourceException;
import com.dataflow.ai.business.engine.source.impl.ApiSourceReader;
import com.dataflow.ai.business.engine.source.impl.CsvSourceReader;
import com.dataflow.ai.business.engine.source.impl.DatabaseSourceReader;
import com.dataflow.ai.business.engine.source.impl.KafkaSourceReader;
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
 * 源读取器工厂
 * 根据数据源类型创建对应的读取器
 */
@Slf4j
@Component
public class SourceReaderFactory {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    private final Map<DataSourceType, SourceReader> readers = new HashMap<>();

    @Resource
    private DatabaseSourceReader databaseSourceReader;

    @Resource
    private ApiSourceReader apiSourceReader;

    @Resource
    private CsvSourceReader csvSourceReader;

    @Resource
    private KafkaSourceReader kafkaSourceReader;

    @PostConstruct
    public void init() {
        readers.put(DataSourceType.MYSQL, databaseSourceReader);
        readers.put(DataSourceType.POSTGRES, databaseSourceReader);
        readers.put(DataSourceType.API, apiSourceReader);
        readers.put(DataSourceType.CSV, csvSourceReader);
        readers.put(DataSourceType.KAFKA, kafkaSourceReader);

        log.info("SourceReaderFactory initialized with {} reader types", readers.size());
    }

    /**
     * 根据数据源创建读取器
     */
    public SourceReader createReader(DataSource dataSource) {
        if (dataSource == null) {
            throw new SourceException("DataSource cannot be null");
        }

        DataSourceType type = dataSource.getType();
        SourceReader reader = readers.get(type);

        if (reader == null) {
            throw new SourceException("Unsupported data source type: " + type);
        }

        log.debug("Created source reader for type: {}", type);
        return reader;
    }

    /**
     * 根据数据源ID创建读取器
     */
    public SourceReader createReaderByDataSourceId(String dataSourceId) {
        Optional<DataSource> dataSourceOpt = dataSourceService.findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw new SourceException("Data source not found: " + dataSourceId);
        }
        return createReader(dataSourceOpt.get());
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
        return readers.containsKey(type);
    }

    /**
     * 获取所有支持的数据源类型
     */
    public java.util.Set<DataSourceType> getSupportedTypes() {
        return readers.keySet();
    }
}
