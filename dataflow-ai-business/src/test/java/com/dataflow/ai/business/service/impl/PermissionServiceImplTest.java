package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PermissionServiceImpl 数据源/Pipeline 权限判断单测。
 */

class PermissionServiceImplTest {

    private final PermissionServiceImpl permissionService = new PermissionServiceImpl();

    /**
     * 验证：hasPipelineAccess - 所有者可访问。
     */
    @Test
    @DisplayName("hasPipelineAccess - 所有者可访问")
    void hasPipelineAccess_owner() {
        Pipeline pipeline = Pipeline.builder()
                .ownerId("u1")
                .permissionLevel(Pipeline.PermissionLevel.PRIVATE)
                .build();
        User user = User.builder().id("u1").role(UserRole.DEVELOPER).build();

        assertTrue(permissionService.hasPipelineAccess(pipeline, user));
    }

    /**
     * 验证：hasPipelineAccess - PUBLIC 任意用户。
     */
    @Test
    @DisplayName("hasPipelineAccess - PUBLIC 任意用户")
    void hasPipelineAccess_public() {
        Pipeline pipeline = Pipeline.builder()
                .ownerId("other")
                .permissionLevel(Pipeline.PermissionLevel.PUBLIC)
                .build();
        User user = User.builder().id("u1").role(UserRole.VIEWER).build();

        assertTrue(permissionService.hasPipelineAccess(pipeline, user));
    }

    /**
     * 验证：hasPipelineAccess - PRIVATE 非所有者拒绝。
     */
    @Test
    @DisplayName("hasPipelineAccess - PRIVATE 非所有者拒绝")
    void hasPipelineAccess_privateDenied() {
        Pipeline pipeline = Pipeline.builder()
                .ownerId("owner")
                .permissionLevel(Pipeline.PermissionLevel.PRIVATE)
                .build();
        User user = User.builder().id("u1").role(UserRole.DEVELOPER).build();

        assertFalse(permissionService.hasPipelineAccess(pipeline, user));
    }

    /**
     * 验证：canAccessDataSource - 创建者可访问。
     */
    @Test
    @DisplayName("canAccessDataSource - 创建者可访问")
    void canAccessDataSource_owner() {
        DataSource ds = DataSource.builder().createdBy("u1").build();
        User user = User.builder().id("u1").role(UserRole.VIEWER).build();
        assertTrue(permissionService.canAccessDataSource(ds, user));
    }

    /**
     * 验证：canAccessDataSource - 非创建者拒绝。
     */
    @Test
    @DisplayName("canAccessDataSource - 非创建者拒绝")
    void canAccessDataSource_denied() {
        DataSource ds = DataSource.builder().createdBy("owner").build();
        User user = User.builder().id("u1").role(UserRole.DEVELOPER).build();
        assertFalse(permissionService.canAccessDataSource(ds, user));
    }

    /**
     * 验证：canExecutePipeline - 分析师可执行有权限的 Pipeline。
     */
    @Test
    @DisplayName("canExecutePipeline - 分析师可执行有权限的 Pipeline")
    void canExecutePipeline_analyst() {
        Pipeline pipeline = Pipeline.builder()
                .ownerId("u1")
                .permissionLevel(Pipeline.PermissionLevel.PRIVATE)
                .build();
        User user = User.builder().id("u1").role(UserRole.ANALYST).build();

        assertTrue(permissionService.canExecutePipeline(pipeline, user));
    }
}
