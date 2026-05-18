package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.request.CreateDataSourceRequest;
import com.dataflow.ai.domain.request.UpdateDataSourceRequest;
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

@WebMvcTest(DataSourceController.class)
@Import(TestSecurityConfig.class)
@WithMockUserId("user-001")
class DataSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSourceService dataSourceService;

    @Autowired
    private ObjectMapper objectMapper;

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = DataSource.builder()
                .id("ds-001")
                .name("test-mysql")
                .type(DataSourceType.MYSQL)
                .createdBy("user-001")
                .build();
    }

    @Test
    @DisplayName("POST /v1/data-sources - 创建")
    void create_success() throws Exception {
        CreateDataSourceRequest request = CreateDataSourceRequest.builder()
                .name("test-mysql")
                .type(DataSourceType.MYSQL)
                .connectionConfig(Map.of("url", "jdbc:mysql://localhost/db"))
                .build();
        when(dataSourceService.createDataSource(any(), eq("user-001"))).thenReturn(dataSource);

        mockMvc.perform(post("/v1/data-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ds-001"));
    }

    @Test
    @DisplayName("GET /v1/data-sources - 列表")
    void list_success() throws Exception {
        when(dataSourceService.findByCreatedBy("user-001")).thenReturn(List.of(dataSource));

        mockMvc.perform(get("/v1/data-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("GET /v1/data-sources/{id} - 详情")
    void get_success() throws Exception {
        when(dataSourceService.findById("ds-001")).thenReturn(Optional.of(dataSource));

        mockMvc.perform(get("/v1/data-sources/ds-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("test-mysql"));
    }

    @Test
    @DisplayName("PUT /v1/data-sources/{id} - 更新")
    void update_success() throws Exception {
        UpdateDataSourceRequest request = UpdateDataSourceRequest.builder().name("renamed").build();
        when(dataSourceService.updateDataSource(eq("ds-001"), any())).thenReturn(dataSource);

        mockMvc.perform(put("/v1/data-sources/ds-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /v1/data-sources/{id} - 删除")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/v1/data-sources/ds-001"))
                .andExpect(status().isOk());

        verify(dataSourceService).deleteDataSource("ds-001");
    }

    @Test
    @DisplayName("POST /v1/data-sources/{id}/test - 连接测试（当前 Service 为占位实现）")
    void testConnection_success() throws Exception {
        when(dataSourceService.testConnection("ds-001")).thenReturn(true);

        mockMvc.perform(post("/v1/data-sources/ds-001/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /v1/data-sources/{id}/preview - 预览（当前 Service 为占位实现）")
    void preview_success() throws Exception {
        when(dataSourceService.previewSourceData("ds-001", "t1", null, 10))
                .thenReturn(Map.of());

        mockMvc.perform(post("/v1/data-sources/ds-001/preview")
                        .param("tableName", "t1")
                        .param("sampleSize", "10"))
                .andExpect(status().isOk());
    }
}
