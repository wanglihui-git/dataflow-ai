package com.dataflow.ai.business.permission;

import com.dataflow.ai.business.config.PermissionProperties;
import com.dataflow.ai.business.permission.impl.PermissionEngineImpl;
import com.dataflow.ai.business.permission.processor.DefaultMaskProcessor;
import com.dataflow.ai.business.repository.FieldPermissionRepository;
import com.dataflow.ai.business.repository.RowPermissionRepository;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.AccessType;
import com.dataflow.ai.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionEngineImplTest {

    @Mock
    private PermissionProperties permissionProperties;
    @Mock
    private FieldPermissionRepository fieldPermissionRepository;
    @Mock
    private RowPermissionRepository rowPermissionRepository;
    @Spy
    private DefaultMaskProcessor maskProcessor = new DefaultMaskProcessor();

    @InjectMocks
    private PermissionEngineImpl permissionEngine;

    @Test
    @DisplayName("MASKED 字段脱敏")
    void masksField() {
        when(permissionProperties.isEnabled()).thenReturn(true);
        when(fieldPermissionRepository.findMatchingRules(any(), any(), any(), any(), any()))
                .thenReturn(List.of(DataFieldPermission.builder()
                        .accessType(AccessType.MASKED)
                        .maskRule("PHONE")
                        .build()));

        User user = User.builder().id("u1").role(UserRole.DEVELOPER).build();
        Map<String, Object> row = new java.util.HashMap<>(Map.of("phone", "13812345678"));
        Map<String, Object> result = permissionEngine.applyPermissions(row, "ds-1", user);

        assertEquals("138****5678", result.get("phone"));
    }
}
