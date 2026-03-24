package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;
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
        // 加密敏感配置
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

        // 更新字段
        if (request.getName() != null) {
            dataSource.setName(request.getName());
        }
        if (request.getType() != null) {
            dataSource.setType(request.getType());
        }
        if (request.getConnectionConfig() != null) {
            // 加密敏感配置
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
        Optional<DataSource> dataSourceOpt = dataSourceRepository.findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw new RuntimeException("数据源不存在");
        }
        DataSource dataSource = dataSourceOpt.get();
        // 解密配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        // TODO: 实现实际的连接测试逻辑
        log.info("Testing connection to datasource: {}", dataSource.getName());
        return true;
    }

    @Override
    public Map<String, Object> previewSourceData(String dataSourceId, String tableName, String query, int sampleSize) {
        Optional<DataSource> dataSourceOpt = dataSourceRepository.findById(dataSourceId);
        if (dataSourceOpt.isEmpty()) {
            throw new RuntimeException("数据源不存在");
        }
        DataSource dataSource = dataSourceOpt.get();
        // 解密配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        // TODO: 实现实际的数据预览逻辑
        log.info("Previewing data from datasource: {}, table: {}, sampleSize: {}", dataSource.getName(), tableName, sampleSize);
        return new HashMap<>();
    }
}
