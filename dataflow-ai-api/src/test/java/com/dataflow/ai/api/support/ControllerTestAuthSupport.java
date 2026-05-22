package com.dataflow.ai.api.support;

import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Controller 层 WebMvc 测试共用的鉴权 Mock 辅助类。
 * <p>统一桩当前用户与数据源/Pipeline 各项权限为允许，避免重复编写 when 语句。</p>
 */
public final class ControllerTestAuthSupport {

    /** 与 {@link WithMockUserId} 默认 principal 一致的用户 ID */
    public static final String TEST_USER_ID = "user-001";

    private ControllerTestAuthSupport() {
    }

    /**
     * 构造用于鉴权桩的测试用户（DEVELOPER 角色）。
     *
     * @return 最小字段填充的 {@link User}
     */
    public static User testUser() {
        return User.builder().id(TEST_USER_ID).role(UserRole.DEVELOPER).build();
    }

    /**
     * 为 Controller 测试注入通用鉴权通过桩。
     *
     * @param userService       用户服务 Mock
     * @param permissionService 权限服务 Mock
     */
    public static void stubAuth(UserService userService, PermissionService permissionService) {
        // 当前用户存在
        when(userService.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser()));
        // 数据源与 Pipeline 各项操作均放行
        when(permissionService.canAccessDataSource(any(DataSource.class), any(User.class))).thenReturn(true);
        when(permissionService.canModifyDataSource(any(DataSource.class), any(User.class))).thenReturn(true);
        when(permissionService.hasPipelineAccess(any(Pipeline.class), any(User.class))).thenReturn(true);
        when(permissionService.canModifyPipeline(any(Pipeline.class), any(User.class))).thenReturn(true);
        when(permissionService.canExecutePipeline(any(Pipeline.class), any(User.class))).thenReturn(true);
        when(permissionService.canDeletePipeline(any(Pipeline.class), any(User.class))).thenReturn(true);
    }
}
