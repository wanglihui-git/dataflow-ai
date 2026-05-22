package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.business.permission.PermissionEngine;
import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.business.repository.UserRepository;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.business.util.RecordPreviewMapper;
import com.dataflow.ai.common.utils.SecurityUtils;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.User;
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
 * {@link DataSourceService} 实现：连接配置加密存储、连接测试与带权限脱敏的预览。
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

    @Resource
    private PermissionEngine permissionEngine;

    @Resource
    private UserRepository userRepository;

    /** {@inheritDoc} */
    @Override
    public Optional<DataSource> findById(String id) {
        return dataSourceRepository.findById(id);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataSource> findByCreatedBy(String createdBy) {
        return dataSourceRepository.findByCreatedBy(createdBy);
    }

    /** {@inheritDoc} */
    @Override
    public List<DataSource> findAll() {
        return dataSourceRepository.findAll();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
            Map<String, Object> merged = mergeConnectionConfig(
                    dataSource.getConnectionConfig(), request.getConnectionConfig());
            dataSource.setConnectionConfig(encryptionService.encrypt(merged));
        }

        dataSource.setUpdatedAt(LocalDateTime.now());
        return dataSourceRepository.save(dataSource);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteDataSource(String id) {
        dataSourceRepository.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    public boolean testConnection(String dataSourceId) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        log.info("Testing connection to datasource: {}", dataSource.getName());
        SourceReader reader = sourceReaderFactory.createReader(dataSource);
        return reader.testConnection(dataSource);
    }

    /** {@inheritDoc} */
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
            applyPreviewPermissions(result, dataSourceId);
            return result;
        } catch (Exception e) {
            log.error("Preview failed for datasource {}: {}", dataSourceId, e.getMessage(), e);
            throw new RuntimeException("数据预览失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对预览结果中的 rows 应用列/行级权限脱敏（无登录用户时跳过）。
     *
     * @param result       预览结果 Map，原地修改 rows
     * @param dataSourceId 数据源 ID
     */
    @SuppressWarnings("unchecked")
    private void applyPreviewPermissions(Map<String, Object> result, String dataSourceId) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return;
            }
            Object rowsObj = result.get("rows");
            if (rowsObj instanceof List<?> rows) {
                List<Map<String, Object>> rowMaps = (List<Map<String, Object>>) rows;
                List<Map<String, Object>> masked = permissionEngine.applyPermissions(rowMaps, dataSourceId, userOpt.get());
                result.put("rows", masked);
                result.put("rowCount", masked.size());
            }
        } catch (Exception e) {
            log.debug("Skip preview permission masking: {}", e.getMessage());
        }
    }

    /**
     * 将请求中的连接配置补丁合并到已有配置（先解密再覆盖同名键，最后整体加密落库）。
     *
     * @param existingEncrypted 库中已加密的连接配置，可为 null
     * @param patch             请求体中的部分连接配置（明文）
     * @return 合并后的明文连接配置
     */
    private Map<String, Object> mergeConnectionConfig(
            Map<String, Object> existingEncrypted, Map<String, Object> patch) {
        Map<String, Object> merged = new HashMap<>();
        Map<String, Object> existingPlain = encryptionService.decrypt(existingEncrypted);
        if (existingPlain != null) {
            merged.putAll(existingPlain);
        }
        merged.putAll(patch);
        return merged;
    }

    /**
     * 根据表名或自定义 SQL 构建预览用 {@link SourceConfig}。
     *
     * @param dataSource 数据源实体
     * @param tableName  表名，可为 null
     * @param query      自定义查询，可为 null
     * @param sampleSize 采样行数（用于 LIMIT）
     * @return 预览源配置
     */
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
