package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.ControllerTestAuthSupport;
import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.business.service.DataPermissionService;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.AccessType;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DataPermissionController 列/行权限 CRUD 测试。
 */

@WebMvcTest
@Import({DataPermissionController.class, TestSecurityConfig.class})
@WithMockUserId("user-001")
class DataPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataPermissionService dataPermissionService;

    @MockBean
    private DataSourceService dataSourceService;

    @MockBean
    private UserService userService;

    @MockBean
    private PermissionService permissionService;

    @Autowired
    private ObjectMapper objectMapper;

    private DataSource dataSource;
    private DataFieldPermission columnPermission;
    private DataRowPermission rowPermission;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() {
        ControllerTestAuthSupport.stubAuth(userService, permissionService);
        dataSource = DataSource.builder()
                .id("ds-001")
                .name("test-mysql")
                .type(DataSourceType.MYSQL)
                .createdBy(ControllerTestAuthSupport.TEST_USER_ID)
                .build();
        // 准备：配置 Mock 返回值
        when(dataSourceService.findById("ds-001")).thenReturn(Optional.of(dataSource));

        columnPermission = DataFieldPermission.builder()
                .id("cp-001")
                .dataSourceId("ds-001")
                .columnName("salary")
                .targetRole(UserRole.ANALYST)
                .accessType(AccessType.MASKED)
                .maskRule("****")
                .build();
        rowPermission = DataRowPermission.builder()
                .id("rp-001")
                .dataSourceId("ds-001")
                .targetRole(UserRole.VIEWER)
                .filterCondition("dept_id = 'SALES'")
                .priority(10)
                .build();
    }

    /**
     * 验证：GET /v1/data-sources/{id}/column-permissions - 列表。
     */
    @Test
    @DisplayName("GET /v1/data-sources/{id}/column-permissions - 列表")
    void listColumnPermissions_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataPermissionService.listColumnPermissions("ds-001")).thenReturn(List.of(columnPermission));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/data-sources/ds-001/column-permissions"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].columnName").value("salary"));
    }

    /**
     * 验证：POST /v1/data-sources/{id}/column-permissions - 创建。
     */
    @Test
    @DisplayName("POST /v1/data-sources/{id}/column-permissions - 创建")
    void createColumnPermission_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataPermissionService.saveColumnPermission(any(DataFieldPermission.class)))
                .thenReturn(columnPermission);

        DataFieldPermission body = DataFieldPermission.builder()
                .columnName("salary")
                .targetRole(UserRole.ANALYST)
                .accessType(AccessType.MASKED)
                .maskRule("****")
                .build();

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/data-sources/ds-001/column-permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("cp-001"));

        // 断言：校验响应或交互
        verify(dataPermissionService).saveColumnPermission(any(DataFieldPermission.class));
    }

    /**
     * 验证：DELETE /v1/data-sources/{id}/column-permissions/{id} - 删除。
     */
    @Test
    @DisplayName("DELETE /v1/data-sources/{id}/column-permissions/{id} - 删除")
    void deleteColumnPermission_success() throws Exception {
        // 准备：配置 Mock 返回值
        doNothing().when(dataPermissionService).deleteColumnPermission("cp-001");

        // 执行：发起 HTTP 请求
        mockMvc.perform(delete("/v1/data-sources/ds-001/column-permissions/cp-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 断言：校验响应或交互
        verify(dataPermissionService).deleteColumnPermission("cp-001");
    }

    /**
     * 验证：GET /v1/data-sources/{id}/row-permissions - 列表。
     */
    @Test
    @DisplayName("GET /v1/data-sources/{id}/row-permissions - 列表")
    void listRowPermissions_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataPermissionService.listRowPermissions("ds-001")).thenReturn(List.of(rowPermission));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/data-sources/ds-001/row-permissions"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].filterCondition").value("dept_id = 'SALES'"));
    }

    /**
     * 验证：POST /v1/data-sources/{id}/row-permissions - 创建。
     */
    @Test
    @DisplayName("POST /v1/data-sources/{id}/row-permissions - 创建")
    void createRowPermission_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataPermissionService.saveRowPermission(any(DataRowPermission.class)))
                .thenReturn(rowPermission);

        DataRowPermission body = DataRowPermission.builder()
                .targetRole(UserRole.VIEWER)
                .filterCondition("dept_id = 'SALES'")
                .priority(10)
                .build();

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/data-sources/ds-001/row-permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("rp-001"));
    }

    /**
     * 验证：DELETE /v1/data-sources/{id}/row-permissions/{id} - 删除。
     */
    @Test
    @DisplayName("DELETE /v1/data-sources/{id}/row-permissions/{id} - 删除")
    void deleteRowPermission_success() throws Exception {
        // 准备：配置 Mock 返回值
        doNothing().when(dataPermissionService).deleteRowPermission("rp-001");

        // 执行：发起 HTTP 请求
        mockMvc.perform(delete("/v1/data-sources/ds-001/row-permissions/rp-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk());

        verify(dataPermissionService).deleteRowPermission("rp-001");
    }
}
