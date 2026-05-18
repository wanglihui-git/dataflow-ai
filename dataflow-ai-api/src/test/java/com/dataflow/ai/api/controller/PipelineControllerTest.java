package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.api.support.WithMockUserId;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
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

@WebMvcTest(PipelineController.class)
@Import(TestSecurityConfig.class)
@WithMockUserId("user-001")
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PipelineService pipelineService;

    @Autowired
    private ObjectMapper objectMapper;

    private Pipeline pipeline;
    private ExecutionRun executionRun;

    @BeforeEach
    void setUp() {
        pipeline = Pipeline.builder().id("pipe-001").name("demo").ownerId("user-001").build();
        executionRun = ExecutionRun.builder()
                .id("run-001")
                .pipelineId("pipe-001")
                .status(ExecutionStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("POST /v1/pipelines - 创建")
    void create_success() throws Exception {
        CreatePipelineRequest request = CreatePipelineRequest.builder().name("demo").build();
        when(pipelineService.createPipeline(any(), eq("user-001"))).thenReturn(pipeline);

        mockMvc.perform(post("/v1/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("pipe-001"));
    }

    @Test
    @DisplayName("GET /v1/pipelines - 列表")
    void list_success() throws Exception {
        when(pipelineService.findByUser("user-001")).thenReturn(List.of(pipeline));

        mockMvc.perform(get("/v1/pipelines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("GET /v1/pipelines/{id} - 详情")
    void get_success() throws Exception {
        when(pipelineService.findById("pipe-001")).thenReturn(Optional.of(pipeline));

        mockMvc.perform(get("/v1/pipelines/pipe-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("demo"));
    }

    @Test
    @DisplayName("PUT /v1/pipelines/{id} - 更新")
    void update_success() throws Exception {
        when(pipelineService.updatePipeline(eq("pipe-001"), any())).thenReturn(pipeline);

        mockMvc.perform(put("/v1/pipelines/pipe-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pipeline)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /v1/pipelines/{id} - 删除")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/v1/pipelines/pipe-001"))
                .andExpect(status().isOk());

        verify(pipelineService).deletePipeline("pipe-001");
    }

    @Test
    @DisplayName("POST /v1/pipelines/{id}/run - 执行")
    void run_success() throws Exception {
        when(pipelineService.executePipeline("pipe-001", "user-001")).thenReturn(executionRun);

        mockMvc.perform(post("/v1/pipelines/pipe-001/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("run-001"));
    }

    @Test
    @DisplayName("GET /v1/pipelines/{id}/runs - 执行历史")
    void getRuns_success() throws Exception {
        when(pipelineService.findExecutionRuns("pipe-001")).thenReturn(List.of(executionRun));

        mockMvc.perform(get("/v1/pipelines/pipe-001/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("GET /v1/pipelines/{id}/preview - 预览（Service 占位）")
    void preview_success() throws Exception {
        when(pipelineService.findById("pipe-001")).thenReturn(Optional.of(pipeline));
        when(pipelineService.previewTransform(pipeline, 10)).thenReturn(Map.of());

        mockMvc.perform(get("/v1/pipelines/pipe-001/preview"))
                .andExpect(status().isOk());
    }
}
