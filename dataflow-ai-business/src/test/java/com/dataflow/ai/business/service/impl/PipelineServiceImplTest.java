package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.preview.PipelinePreviewExecutor;
import com.dataflow.ai.business.repository.PipelineRepository;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import com.dataflow.ai.domain.request.CreatePipelineRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineServiceImplTest {

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private ExecutionService executionService;

    @Mock
    private PipelinePreviewExecutor pipelinePreviewExecutor;

    @InjectMocks
    private PipelineServiceImpl pipelineService;

    @Test
    @DisplayName("createPipeline - 保存并设置 owner")
    void createPipeline_setsOwner() {
        when(pipelineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Pipeline pipeline = pipelineService.createPipeline(
                CreatePipelineRequest.builder().name("p1").build(), "user-001");

        assertEquals("user-001", pipeline.getOwnerId());
        verify(pipelineRepository).save(any());
    }

    @Test
    @DisplayName("executePipeline - 创建 run 并异步启动")
    void executePipeline_createsRunAndStarts() {
        Pipeline pipeline = Pipeline.builder().id("pipe-1").name("p").build();
        ExecutionRun run = ExecutionRun.builder().id("run-1").status(ExecutionStatus.PENDING).build();
        when(pipelineRepository.findById("pipe-1")).thenReturn(Optional.of(pipeline));
        when(executionService.createExecutionRun("pipe-1", "user-001")).thenReturn(run);

        ExecutionRun result = pipelineService.executePipeline("pipe-1", "user-001");

        assertEquals("run-1", result.getId());
        verify(executionService).startExecution(eq("run-1"), eq(pipeline));
    }

    @Test
    @DisplayName("previewTransform - 委托 PipelinePreviewExecutor")
    void previewTransform_delegatesToExecutor() throws Exception {
        Pipeline pipeline = Pipeline.builder().id("pipe-1").name("p").build();
        when(pipelinePreviewExecutor.preview(pipeline, 10))
                .thenReturn(Map.of("columns", List.of("id"), "rows", List.of(), "rowCount", 0));

        Map<String, Object> result = pipelineService.previewTransform(pipeline, 10);

        assertEquals(0, result.get("rowCount"));
        verify(pipelinePreviewExecutor).preview(pipeline, 10);
    }
}
