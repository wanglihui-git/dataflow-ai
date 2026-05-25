package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;

/**
 * 资源访问权限服务接口。
 * <p>基于用户角色与 Pipeline 共享配置判断对数据源、Pipeline 的读/写/执行权限。</p>
 */
public interface PermissionService {

    /**
     * 判断用户是否可访问数据源（所有者为本人或管理员）。
     *
     * @param dataSource 数据源
     * @param user       当前用户
     * @return 有访问权返回 true
     */
    boolean canAccessDataSource(DataSource dataSource, User user);

    /**
     * 判断用户是否可修改或删除数据源（需开发者及以上且为所有者或管理员）。
     *
     * @param dataSource 数据源
     * @param user       当前用户
     * @return 可修改返回 true
     */
    boolean canModifyDataSource(DataSource dataSource, User user);

    /**
     * 判断用户是否可查看 Pipeline（所有者、公开或共享名单命中）。
     *
     * @param pipeline Pipeline
     * @param user     当前用户
     * @return 有访问权返回 true
     */
    boolean hasPipelineAccess(Pipeline pipeline, User user);

    /**
     * 判断用户是否可编辑 Pipeline 配置。
     *
     * @param pipeline Pipeline
     * @param user     当前用户
     * @return 可修改返回 true
     */
    boolean canModifyPipeline(Pipeline pipeline, User user);

    /**
     * 判断用户是否可触发 Pipeline 执行。
     *
     * @param pipeline Pipeline
     * @param user     当前用户
     * @return 可执行返回 true
     */
    boolean canExecutePipeline(Pipeline pipeline, User user);

    /**
     * 判断用户是否可删除 Pipeline。
     *
     * @param pipeline Pipeline
     * @param user     当前用户
     * @return 可删除返回 true
     */
    boolean canDeletePipeline(Pipeline pipeline, User user);

    /**
     * 是否为管理员角色。
     *
     * @param user 当前用户
     * @return ADMIN 返回 true
     */
    boolean hasAdminRole(User user);

    /**
     * 是否为开发者及以上（ADMIN、DEVELOPER）。
     *
     * @param user 当前用户
     * @return 满足则 true
     */
    boolean hasDeveloperRole(User user);

    /**
     * 是否为分析师及以上（含 ANALYST）。
     *
     * @param user 当前用户
     * @return 满足则 true
     */
    boolean hasAnalystRole(User user);

    /**
     * 是否为查看者及以上（含 VIEWER，即任意有效角色）。
     *
     * @param user 当前用户
     * @return 满足则 true
     */
    boolean hasViewerRole(User user);
}
