package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import org.springframework.stereotype.Service;

/**
 * {@link PermissionService} 实现：基于角色与 Pipeline 共享名单的访问控制判断。
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    /** {@inheritDoc} */
    @Override
    public boolean canAccessDataSource(DataSource dataSource, User user) {
        if (hasAdminRole(user)) {
            return true;
        }
        return dataSource.getCreatedBy() != null && dataSource.getCreatedBy().equals(user.getId());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canModifyDataSource(DataSource dataSource, User user) {
        return canAccessDataSource(dataSource, user) && hasDeveloperRole(user);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPipelineAccess(Pipeline pipeline, User user) {
        // 管理员可以访问所有Pipeline
        if (hasAdminRole(user)) {
            return true;
        }
        // 所有者可以访问自己的Pipeline
        if (pipeline.getOwnerId().equals(user.getId())) {
            return true;
        }
        // 公开Pipeline可以访问
        if (pipeline.getPermissionLevel() == Pipeline.PermissionLevel.PUBLIC) {
            return true;
        }
        if (pipeline.getPermissionLevel() == Pipeline.PermissionLevel.SHARED) {
            return checkSharedAccess(pipeline, user);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canModifyPipeline(Pipeline pipeline, User user) {
        // 管理员可以修改所有Pipeline
        if (hasAdminRole(user)) {
            return true;
        }
        // 所有者可以修改自己的Pipeline
        if (pipeline.getOwnerId().equals(user.getId()) && hasDeveloperRole(user)) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecutePipeline(Pipeline pipeline, User user) {
        // 管理员可以执行所有Pipeline
        if (hasAdminRole(user)) {
            return true;
        }
        // 分析师及以上角色可以执行有权限的Pipeline
        if (hasAnalystRole(user) && hasPipelineAccess(pipeline, user)) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean canDeletePipeline(Pipeline pipeline, User user) {
        // 管理员可以删除所有Pipeline
        if (hasAdminRole(user)) {
            return true;
        }
        // 所有者可以删除自己的Pipeline
        if (pipeline.getOwnerId().equals(user.getId()) && hasDeveloperRole(user)) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAdminRole(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDeveloperRole(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.DEVELOPER;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasAnalystRole(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.DEVELOPER || user.getRole() == UserRole.ANALYST;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasViewerRole(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.DEVELOPER
                || user.getRole() == UserRole.ANALYST || user.getRole() == UserRole.VIEWER;
    }

    /**
     * 判断用户是否命中 SHARED Pipeline 的角色/用户/部门白名单。
     *
     * @param pipeline Pipeline（permissionLevel 应为 SHARED）
     * @param user     当前用户
     * @return 在白名单内返回 true
     */
    private boolean checkSharedAccess(Pipeline pipeline, User user) {
        if (pipeline.getAllowedRoles() != null && !pipeline.getAllowedRoles().isEmpty()) {
            if (pipeline.getAllowedRoles().contains(user.getRole().name())) {
                return true;
            }
        }
        if (pipeline.getAllowedUsers() != null && !pipeline.getAllowedUsers().isEmpty()) {
            if (pipeline.getAllowedUsers().contains(user.getId())) {
                return true;
            }
        }
        if (pipeline.getAllowedDepartments() != null && !pipeline.getAllowedDepartments().isEmpty()) {
            if (user.getDepartment() != null && pipeline.getAllowedDepartments().contains(user.getDepartment())) {
                return true;
            }
        }
        return false;
    }
}
