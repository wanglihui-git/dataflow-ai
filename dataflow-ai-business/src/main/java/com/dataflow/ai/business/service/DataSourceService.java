package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.vo.ConnectionTestResult;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源管理服务接口。
 * <p>负责数据源 CRUD、连接测试及带权限脱敏的数据预览。</p>
 */
public interface DataSourceService {

    /**
     * 按 ID 查询数据源。
     *
     * @param id 数据源 ID
     * @return 数据源实体，不存在则为空
     */
    Optional<DataSource> findById(String id);

    /**
     * 查询指定用户创建的数据源列表。
     *
     * @param createdBy 创建者用户 ID
     * @return 数据源列表
     */
    List<DataSource> findByCreatedBy(String createdBy);

    /**
     * 查询全部数据源（通常仅管理员使用）。
     *
     * @return 数据源列表
     */
    List<DataSource> findAll();

    /**
     * 创建数据源（连接配置经加密后存储）。
     *
     * @param request   创建请求
     * @param createdBy 创建者用户 ID
     * @return 持久化后的数据源
     */
    DataSource createDataSource(CreateDataSourceRequest request, String createdBy);

    /**
     * 部分更新数据源名称、类型或连接配置。
     *
     * @param id      数据源 ID
     * @param request 更新字段
     * @return 更新后的数据源
     */
    DataSource updateDataSource(String id, UpdateDataSourceRequest request);

    /**
     * 按 ID 删除数据源。
     *
     * @param id 数据源 ID
     */
    void deleteDataSource(String id);

    /**
     * 测试数据源连接是否可用。
     *
     * @param dataSourceId 数据源 ID
     * @return 连通性测试结果（含失败原因）
     */
    ConnectionTestResult testConnection(String dataSourceId);

    /**
     * 预览数据源样本数据（应用行/列权限脱敏）。
     *
     * @param dataSourceId 数据源 ID
     * @param tableName    表名，与 query 二选一
     * @param query        自定义 SQL，与 tableName 二选一
     * @param sampleSize   采样行数
     * @return 含 rows、columns 等字段的预览 Map
     */
    Map<String, Object> previewSourceData(String dataSourceId, String tableName, String query, int sampleSize);
}
