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
 * Controller 测试鉴权 Mock 辅助
 */
public final class ControllerTestAuthSupport {

    public static final String TEST_USER_ID = "user-001";

    private ControllerTestAuthSupport() {
    }

    public static User testUser() {
        return User.builder().id(TEST_USER_ID).role(UserRole.DEVELOPER).build();
    }

    public static void stubAuth(UserService userService, PermissionService permissionService) {
        when(userService.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser()));
        when(permissionService.canAccessDataSource(any(DataSource.class), any(User.class))).thenReturn(true);
        when(permissionService.canModifyDataSource(any(DataSource.class), any(User.class))).thenReturn(true);
        when(permissionService.hasPipelineAccess(any(Pipeline.class), any(User.class))).thenReturn(true);
        when(permissionService.canModifyPipeline(any(Pipeline.class), any(User.class))).thenReturn(true);
        when(permissionService.canExecutePipeline(any(Pipeline.class), any(User.class))).thenReturn(true);
        when(permissionService.canDeletePipeline(any(Pipeline.class), any(User.class))).thenReturn(true);
    }
}
