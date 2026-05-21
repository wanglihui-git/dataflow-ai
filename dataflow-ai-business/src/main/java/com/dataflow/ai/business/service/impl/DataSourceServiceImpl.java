package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.business.util.RecordPreviewMapper;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 数据源服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    @Resource
    private DataSourceRepository dataSourceRepository;

    @Resource
    private EncryptionService encryptionService;

    @Resource
    private SourceReaderFactory sourceReaderFactory;

    @Override
    public Optional<DataSource> findById(String id) {
        return dataSourceRepository.findById(id);
    }

    @Override
    public List<DataSource> findByCreatedBy(String createdBy) {
        return dataSourceRepository.findByCreatedBy(createdBy);
    }

    @Override
    public List<DataSource> findAll() {
        return dataSourceRepository.findAll();
    }

    @Override
    public DataSource createDataSource(CreateDataSourceRequest request, String createdBy) {
        Map<String, Object> encryptedConfig = encryptionService.encrypt(request.getConnectionConfig());
        DataSource dataSource = DataSource.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .type(request.getType())
                .connectionConfig(encryptedConfig)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return dataSourceRepository.save(dataSource);
    }

    @Override
    public DataSource updateDataSource(String id, UpdateDataSourceRequest request) {
        Optional<DataSource> existingOpt = dataSourceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("数据源不存在");
        }
        DataSource dataSource = existingOpt.get();

        if (request.getName() != null) {
            dataSource.setName(request.getName());
        }
        if (request.getType() != null) {
            dataSource.setType(request.getType());
        }
        if (request.getConnectionConfig() != null) {
            Map<String, Object> encryptedConfig = encryptionService.encrypt(request.getConnectionConfig());
            dataSource.setConnectionConfig(encryptedConfig);
        }

        dataSource.setUpdatedAt(LocalDateTime.now());
        return dataSourceRepository.save(dataSource);
    }

    @Override
    public void deleteDataSource(String id) {
        dataSourceRepository.deleteById(id);
    }

    @Override
    public boolean testConnection(String dataSourceId) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        log.info("Testing connection to datasource: {}", dataSource.getName());
        SourceReader reader = sourceReaderFactory.createReader(dataSource);
        return reader.testConnection(dataSource);
    }

    @Override
    public Map<String, Object> previewSourceData(String dataSourceId, String tableName, String query, int sampleSize) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        if (sampleSize <= 0) {
            sampleSize = 10;
        }
        log.info("Previewing data from datasource: {}, table: {}, sampleSize: {}",
                dataSource.getName(), tableName, sampleSize);

        SourceConfig sourceConfig = buildPreviewSourceConfig(dataSource, tableName, query, sampleSize);
        try {
            SourceReader reader = sourceReaderFactory.createReader(dataSource);
            List<Record> records = reader.preview(sourceConfig, sampleSize);
            Map<String, Object> result = new HashMap<>(RecordPreviewMapper.toPreviewMap(records));
            result.put("dataSourceId", dataSourceId);
            result.put("type", dataSource.getType().name());
            return result;
        } catch (Exception e) {
            log.error("Preview failed for datasource {}: {}", dataSourceId, e.getMessage(), e);
            throw new RuntimeException("数据预览失败: " + e.getMessage(), e);
        }
    }

    private SourceConfig buildPreviewSourceConfig(DataSource dataSource, String tableName, String query, int sampleSize) {
        SourceConfig.SourceConfigBuilder builder = SourceConfig.builder()
                .dataSourceId(dataSource.getId())
                .type(dataSource.getType());
        if (query != null && !query.isBlank()) {
            builder.query(query.trim());
        } else if (tableName != null && !tableName.isBlank()) {
            builder.tableName(tableName.trim());
            builder.query("SELECT * FROM " + tableName.trim() + " LIMIT " + sampleSize);
        } else {
            throw new RuntimeException("预览需要指定 tableName 或 query 参数");
        }
        return builder.build();
    }
}
