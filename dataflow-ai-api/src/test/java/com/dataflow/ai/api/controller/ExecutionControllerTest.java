package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({ExecutionController.class, TestSecurityConfig.class})
@WithMockUserId("user-001")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionService executionService;

    private ExecutionRun executionRun;

    @BeforeEach
    void setUp() {
        executionRun = ExecutionRun.builder()
                .id("run-001")
                .pipelineId("pipe-001")
                .status(ExecutionStatus.RUNNING)
                .build();
    }

    @Test
    @DisplayName("GET /v1/execution/runs/{runId} - 详情")
    void getRun_success() throws Exception {
        when(executionService.findById("run-001")).thenReturn(Optional.of(executionRun));

        mockMvc.perform(get("/v1/execution/runs/run-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("run-001"));
    }

    @Test
    @DisplayName("POST /v1/execution/runs/{runId}/cancel - 取消")
    void cancel_success() throws Exception {
        mockMvc.perform(post("/v1/execution/runs/run-001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(executionService).cancelExecution("run-001");
    }

    @Test
    @DisplayName("GET /v1/execution/pipelines/{pipelineId}/stats - 统计")
    void getStats_success() throws Exception {
        when(executionService.getExecutionStats("pipe-001"))
                .thenReturn(Map.of("total", 10, "success", 8, "failed", 2, "successRate", 0.8));

        mockMvc.perform(get("/v1/execution/pipelines/pipe-001/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(10))
                .andExpect(jsonPath("$.data.successRate").value(0.8));
    }
}
