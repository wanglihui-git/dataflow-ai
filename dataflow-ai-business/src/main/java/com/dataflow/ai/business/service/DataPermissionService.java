package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;

import java.util.List;

/**
 * 数据权限配置服务。
 * <p>管理数据源上的列级（字段脱敏/隐藏）与行级（过滤条件）权限规则 CRUD。</p>
 */
public interface DataPermissionService {

    /**
     * 查询数据源下全部列级权限规则。
     *
     * @param dataSourceId 数据源 ID
     * @return 列权限列表，无记录时为空列表
     */
    List<DataFieldPermission> listColumnPermissions(String dataSourceId);

    /**
     * 新增或更新列级权限规则。
     *
     * @param permission 权限实体（含 id 时为更新）
     * @return 持久化后的实体
     */
    DataFieldPermission saveColumnPermission(DataFieldPermission permission);

    /**
     * 按主键删除列级权限规则。
     *
     * @param id 权限记录 ID
     */
    void deleteColumnPermission(String id);

    /**
     * 查询数据源下全部行级权限规则。
     *
     * @param dataSourceId 数据源 ID
     * @return 行权限列表
     */
    List<DataRowPermission> listRowPermissions(String dataSourceId);

    /**
     * 新增或更新行级权限规则。
     *
     * @param permission 权限实体
     * @return 持久化后的实体
     */
    DataRowPermission saveRowPermission(DataRowPermission permission);

    /**
     * 按主键删除行级权限规则。
     *
     * @param id 权限记录 ID
     */
    void deleteRowPermission(String id);
}
