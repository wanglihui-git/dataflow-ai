package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.DataRowPermission;

import java.util.List;
import java.util.Optional;

/**
 * 数据行级权限仓储接口
 */
public interface RowPermissionRepository {

    /** 根据数据源 ID 查询行权限规则 */
    List<DataRowPermission> findByDataSourceId(String dataSourceId);

    /** 根据 ID 查询 */
    Optional<DataRowPermission> findById(String id);

    /** 保存行权限规则 */
    DataRowPermission save(DataRowPermission permission);

    /** 根据 ID 删除 */
    void deleteById(String id);
}
