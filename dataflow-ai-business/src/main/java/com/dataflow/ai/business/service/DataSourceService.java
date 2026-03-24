package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源服务接口
 */
public interface DataSourceService {

    /**
     * 根据ID查询数据源
     */
    Optional<DataSource> findById(String id);

    /**
     * 根据创建者查询数据源列表
     */
    List<DataSource> findByCreatedBy(String createdBy);

    /**
     * 查询所有数据源
     */
    List<DataSource> findAll();

    /**
     * 创建数据源
     */
    DataSource createDataSource(CreateDataSourceRequest request, String createdBy);

    /**
     * 更新数据源
     */
    DataSource updateDataSource(String id, UpdateDataSourceRequest request);

    /**
     * 删除数据源
     */
    void deleteDataSource(String id);

    /**
     * 测试数据源连接
     */
    boolean testConnection(String dataSourceId);

    /**
     * 预览数据源数据
     */
    Map<String, Object> previewSourceData(String dataSourceId, String tableName, String query, int sampleSize);
}
