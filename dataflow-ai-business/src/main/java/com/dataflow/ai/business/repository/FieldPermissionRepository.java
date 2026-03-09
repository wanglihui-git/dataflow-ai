package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.DataFieldPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

/**
 * 字段权限Repository接口
 * 定义在业务层，实现在基础设施层（依赖倒置）
 */
public interface FieldPermissionRepository {

    /**
     * 根据数据源ID查询所有权限规则
     */
    List<DataFieldPermission> findByDataSourceId(String dataSourceId);

    /**
     * 查询匹配特定用户和字段的规则（按优先级排序）
     */
    List<DataFieldPermission> findMatchingRules(String dataSourceId,
                                                String fieldName,
                                                String userId,
                                                String role,
                                                String department);

    /**
     * 保存规则
     */
    DataFieldPermission save(DataFieldPermission permission);

    /**
     * 删除规则
     */
    void deleteById(String id);

    /**
     * 分页查询
     */
    Page<DataFieldPermission> findByDataSourceId(String dataSourceId,
                                                 Pageable pageable);
}