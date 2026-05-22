package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.api.support.ControllerTestAuthSupport;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.response.PageResponse;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import com.dataflow.ai.domain.request.CreatePipelineRequest;
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
 * PipelineController CRUD、执行与预览接口测试。
 */

@WebMvcTest
@Import({PipelineController.class, TestSecurityConfig.class})
@WithMockUserId("user-001")
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PipelineService pipelineService;

    @MockBean
    private UserService userService;

    @MockBean
    private PermissionService permissionService;

    @Autowired
    private ObjectMapper objectMapper;

    private Pipeline pipeline;
    private ExecutionRun executionRun;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() {
        ControllerTestAuthSupport.stubAuth(userService, permissionService);
        pipeline = Pipeline.builder().id("pipe-001").name("demo").ownerId("user-001").build();
        executionRun = ExecutionRun.builder()
                .id("run-001")
                .pipelineId("pipe-001")
                .status(ExecutionStatus.PENDING)
                .build();
        // 准备：配置 Mock 返回值
        when(pipelineService.findById("pipe-001")).thenReturn(Optional.of(pipeline));
    }

    /**
     * 验证：POST /v1/pipelines - 创建。
     */
    @Test
    @DisplayName("POST /v1/pipelines - 创建")
    void create_success() throws Exception {
        CreatePipelineRequest request = CreatePipelineRequest.builder().name("demo").build();
        // 准备：配置 Mock 返回值
        when(pipelineService.createPipeline(any(), eq("user-001"))).thenReturn(pipeline);

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("pipe-001"));
    }

    /**
     * 验证：GET /v1/pipelines - 列表。
     */
    @Test
    @DisplayName("GET /v1/pipelines - 列表")
    void list_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(pipelineService.findByUserPage(eq("user-001"), any(), any()))
                .thenReturn(PageResponse.of(List.of(pipeline), 0, 20, 1));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/pipelines"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    /**
     * 验证：GET /v1/pipelines/{id} - 详情。
     */
    @Test
    @DisplayName("GET /v1/pipelines/{id} - 详情")
    void get_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(pipelineService.findById("pipe-001")).thenReturn(Optional.of(pipeline));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/pipelines/pipe-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("demo"));
    }

    /**
     * 验证：PUT /v1/pipelines/{id} - 更新。
     */
    @Test
    @DisplayName("PUT /v1/pipelines/{id} - 更新")
    void update_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(pipelineService.updatePipeline(eq("pipe-001"), any())).thenReturn(pipeline);

        // 执行：发起 HTTP 请求
        mockMvc.perform(put("/v1/pipelines/pipe-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pipeline)))
                // 断言：校验响应或交互
                .andExpect(status().isOk());
    }

    /**
     * 验证：DELETE /v1/pipelines/{id} - 删除。
     */
    @Test
    @DisplayName("DELETE /v1/pipelines/{id} - 删除")
    void delete_success() throws Exception {
        // 执行：发起 HTTP 请求
        mockMvc.perform(delete("/v1/pipelines/pipe-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk());

        verify(pipelineService).deletePipeline("pipe-001");
    }

    /**
     * 验证：POST /v1/pipelines/{id}/run - 执行。
     */
    @Test
    @DisplayName("POST /v1/pipelines/{id}/run - 执行")
    void run_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(pipelineService.executePipeline("pipe-001", "user-001")).thenReturn(executionRun);

        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/pipelines/pipe-001/run"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("run-001"));
    }

    /**
     * 验证：GET /v1/pipelines/{id}/runs - 执行历史。
     */
    @Test
    @DisplayName("GET /v1/pipelines/{id}/runs - 执行历史")
    void getRuns_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(pipelineService.findExecutionRuns("pipe-001")).thenReturn(List.of(executionRun));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/pipelines/pipe-001/runs"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    /**
     * 验证：GET /v1/pipelines/{id}/preview - 预览（Service 占位）。
     */
    @Test
    @DisplayName("GET /v1/pipelines/{id}/preview - 预览（Service 占位）")
    void preview_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(pipelineService.findById("pipe-001")).thenReturn(Optional.of(pipeline));
        when(pipelineService.previewTransform(pipeline, 10)).thenReturn(Map.of());

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/pipelines/pipe-001/preview"))
                // 断言：校验响应或交互
                .andExpect(status().isOk());
    }
}
