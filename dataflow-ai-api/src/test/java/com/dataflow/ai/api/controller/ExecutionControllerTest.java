package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.api.support.ControllerTestAuthSupport;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.service.PermissionService;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExecutionController 执行查询、日志与取消测试。
 */

@WebMvcTest
@Import({ExecutionController.class, TestSecurityConfig.class})
@WithMockUserId("user-001")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionService executionService;

    @MockBean
    private PipelineService pipelineService;

    @MockBean
    private UserService userService;

    @MockBean
    private PermissionService permissionService;

    private ExecutionRun executionRun;

    /**
     * 每个用例执行前初始化 Mock 与测试数据。
     */
    @BeforeEach
    void setUp() {
        ControllerTestAuthSupport.stubAuth(userService, permissionService);
        // 准备：配置 Mock 返回值
        when(pipelineService.findById("pipe-001")).thenReturn(Optional.of(
                Pipeline.builder().id("pipe-001").ownerId(ControllerTestAuthSupport.TEST_USER_ID).build()));
        executionRun = ExecutionRun.builder()
                .id("run-001")
                .pipelineId("pipe-001")
                .status(ExecutionStatus.RUNNING)
                .build();
        when(executionService.findById("run-001")).thenReturn(Optional.of(executionRun));
    }

    /**
     * 验证：GET /v1/execution/runs/{runId} - 详情。
     */
    @Test
    @DisplayName("GET /v1/execution/runs/{runId} - 详情")
    void getRun_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(executionService.findById("run-001")).thenReturn(Optional.of(executionRun));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/execution/runs/run-001"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("run-001"));
    }

    /**
     * 验证：POST /v1/execution/runs/{runId}/cancel - 取消。
     */
    @Test
    @DisplayName("POST /v1/execution/runs/{runId}/cancel - 取消")
    void cancel_success() throws Exception {
        // 执行：发起 HTTP 请求
        mockMvc.perform(post("/v1/execution/runs/run-001/cancel"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 断言：校验响应或交互
        verify(executionService).cancelExecution("run-001");
    }

    /**
     * 验证：GET /v1/execution/runs - 分页列表。
     */
    @Test
    @DisplayName("GET /v1/execution/runs - 分页列表")
    void listRuns_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(executionService.findByStatus(eq(ExecutionStatus.SUCCESS), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(executionRun), PageRequest.of(0, 20), 1));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/execution/runs")
                        .param("status", "SUCCESS")
                        .param("page", "0")
                        .param("size", "20"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value("run-001"))
                // 断言：校验响应或交互
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    /**
     * 验证：GET /v1/execution/runs/{runId}/logs - 执行日志。
     */
    @Test
    @DisplayName("GET /v1/execution/runs/{runId}/logs - 执行日志")
    void getRunLogs_success() throws Exception {
        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(Map.of(
                "timestamp", "2026-05-22T10:00:01",
                "phase", "SOURCE",
                "message", "Read 100 records"));
        Map<String, Object> log = new HashMap<>();
        log.put("entries", entries);
        executionRun.setExecutionLog(log);
        // 准备：配置 Mock 返回值
        when(executionService.findById("run-001")).thenReturn(Optional.of(executionRun));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/execution/runs/run-001/logs"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[0].phase").value("SOURCE"));
    }

    /**
     * 验证：GET /v1/execution/pipelines/{pipelineId}/stats - 统计。
     */
    @Test
    @DisplayName("GET /v1/execution/pipelines/{pipelineId}/stats - 统计")
    void getStats_success() throws Exception {
        // 准备：配置 Mock 返回值
        when(executionService.getExecutionStats("pipe-001"))
                .thenReturn(Map.of("total", 10, "success", 8, "failed", 2, "successRate", 0.8));

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/execution/pipelines/pipe-001/stats"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.successRate").value(0.8));
    }
}
