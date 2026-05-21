package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 检查用户是否有权限访问数据源（所有者或管理员）
     */
    boolean canAccessDataSource(DataSource dataSource, User user);

    /**
     * 检查用户是否有权限修改/删除数据源
     */
    boolean canModifyDataSource(DataSource dataSource, User user);

    /**
     * 检查用户是否有权限访问Pipeline
     */
    boolean hasPipelineAccess(Pipeline pipeline, User user);

    /**
     * 检查用户是否有权限修改Pipeline
     */
    boolean canModifyPipeline(Pipeline pipeline, User user);

    /**
     * 检查用户是否有权限执行Pipeline
     */
    boolean canExecutePipeline(Pipeline pipeline, User user);

    /**
     * 检查用户是否有权限删除Pipeline
     */
    boolean canDeletePipeline(Pipeline pipeline, User user);

    /**
     * 检查用户是否有管理员权限
     */
    boolean hasAdminRole(User user);

    /**
     * 检查用户是否有开发者及以上权限
     */
    boolean hasDeveloperRole(User user);

    /**
     * 检查用户是否有分析师及以上权限
     */
    boolean hasAnalystRole(User user);

    /**
     * 检查用户是否有查看权限
     */
    boolean hasViewerRole(User user);
}
