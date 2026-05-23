package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.api.support.ControllerTestAuthSupport;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;
import com.dataflow.ai.domain.vo.ConnectionTestResult;
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
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DataSourceController 测试，Mock 数据源服务与权限。
 */

@WebMvcTest
@Import({DataSourceController.class, TestSecurityConfig.class})
@WithMockUserId("user-001")
class DataSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSourceService dataSourceService;

    @MockBean
    private UserService userService;

    @MockBean
    private PermissionService permissionService;

    @Autowired
    private ObjectMapper objectMapper;

    private DataSource dataSource;

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
                .createdBy("user-001")
                .build();
        // 准备：配置 Mock 返回值
        when(dataSourceService.findById("ds-001")).thenReturn(Optional.of(dataSource));
    }

    /**
     * 验证：POST /v1/data-sources - 创建。
     */
    @Test
    @DisplayName("POST /v1/data-sources - 创建")
    void create_success() throws Exception {
        CreateDataSourceRequest request = CreateDataSourceRequest.builder()
                .name("test-mysql")
                .type(DataSourceType.MYSQL)
                .connectionConfig(Map.of("url", "jdbc:mysql://localhost/db"))
                .build();
        // 准备：配置 Mock 返回值
        when(dataSourceService.createDataSource(any(), eq("user-001"))).thenReturn(dataSource);

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/data-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ds-001"));
    }

    /**
     * 验证：GET /v1/data-sources - 列表。
     */
    @Test
    @DisplayName("GET /v1/data-sources - 列表")
    void list_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataSourceService.findByCreatedBy("user-001")).thenReturn(List.of(dataSource));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/data-sources"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    /**
     * 验证：GET /v1/data-sources/{id} - 详情。
     */
    @Test
    @DisplayName("GET /v1/data-sources/{id} - 详情")
    void get_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataSourceService.findById("ds-001")).thenReturn(Optional.of(dataSource));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/data-sources/ds-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("test-mysql"));
    }

    /**
     * 验证：PUT /v1/data-sources/{id} - 更新。
     */
    @Test
    @DisplayName("PUT /v1/data-sources/{id} - 更新")
    void update_success() throws Exception {
        UpdateDataSourceRequest request = UpdateDataSourceRequest.builder().name("renamed").build();
        // 准备：配置 Mock 返回值
        when(dataSourceService.updateDataSource(eq("ds-001"), any())).thenReturn(dataSource);

        // 执行：发起 HTTP 请求
        mockMvc.perform(put("/v1/data-sources/ds-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk());
    }

    /**
     * 验证：DELETE /v1/data-sources/{id} - 删除。
     */
    @Test
    @DisplayName("DELETE /v1/data-sources/{id} - 删除")
    void delete_success() throws Exception {
        // 执行：发起 HTTP 请求
        mockMvc.perform(delete("/v1/data-sources/ds-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk());

        verify(dataSourceService).deleteDataSource("ds-001");
    }

    /**
     * 验证：POST /v1/data-sources/{id}/test - 连接成功。
     */
    @Test
    @DisplayName("POST /v1/data-sources/{id}/test - 连接成功")
    void testConnection_success() throws Exception {
        when(dataSourceService.testConnection("ds-001"))
                .thenReturn(ConnectionTestResult.success());

        mockMvc.perform(post("/v1/data-sources/ds-001/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.message").value("连接成功"));
    }

    /**
     * 验证：POST /v1/data-sources/{id}/test - 连接失败返回 400 与原因。
     */
    @Test
    @DisplayName("POST /v1/data-sources/{id}/test - 连接失败")
    void testConnection_failure() throws Exception {
        when(dataSourceService.testConnection("ds-001"))
                .thenReturn(ConnectionTestResult.failure("数据库连接失败: Connection refused"));

        mockMvc.perform(post("/v1/data-sources/ds-001/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("数据库连接失败: Connection refused"))
                .andExpect(jsonPath("$.data.connected").value(false));
    }

    /**
     * 验证：POST /v1/data-sources/{id}/preview - 预览（当前 Service 为占位实现）。
     */
    @Test
    @DisplayName("POST /v1/data-sources/{id}/preview - 预览（当前 Service 为占位实现）")
    void preview_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(dataSourceService.previewSourceData("ds-001", "t1", null, 10))
                .thenReturn(Map.of());

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/data-sources/ds-001/preview")
                        .param("tableName", "t1")
                        .param("sampleSize", "10"))
                // 断言：校验响应或交互
                .andExpect(status().isOk());
    }
}
