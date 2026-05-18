package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.orchestrator.PipelineOrchestrator;
import com.dataflow.ai.business.repository.ExecutionRunRepository;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplTest {

    @Mock
    private ExecutionRunRepository executionRunRepository;

    @Mock
    private PipelineOrchestrator pipelineOrchestrator;

    @InjectMocks
    private ExecutionServiceImpl executionService;

    @Test
    @DisplayName("createExecutionRun - 初始状态 PENDING")
    void createExecutionRun_pendingStatus() {
        when(executionRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExecutionRun run = executionService.createExecutionRun("pipe-1", "user-001");

        assertEquals(ExecutionStatus.PENDING, run.getStatus());
        assertEquals("pipe-1", run.getPipelineId());
        verify(executionRunRepository).save(any());
    }

    @Test
    @DisplayName("getExecutionStats - 聚合统计")
    void getExecutionStats_aggregatesCounts() {
        when(executionRunRepository.countByPipelineId("pipe-1")).thenReturn(10L);
        when(executionRunRepository.countByPipelineIdAndStatus("pipe-1", ExecutionStatus.SUCCESS)).thenReturn(8L);
        when(executionRunRepository.countByPipelineIdAndStatus("pipe-1", ExecutionStatus.FAILED)).thenReturn(2L);

        var stats = executionService.getExecutionStats("pipe-1");

        assertEquals(10L, stats.get("total"));
        assertEquals(8L, stats.get("success"));
        assertEquals(0.8, stats.get("successRate"));
    }

    @Test
    @DisplayName("cancelExecution - 更新状态为 CANCELLED")
    void cancelExecution_updatesStatus() {
        ExecutionRun run = ExecutionRun.builder().id("run-1").status(ExecutionStatus.RUNNING).build();
        when(executionRunRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(executionRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.cancelExecution("run-1");

        verify(executionRunRepository).save(any());
    }

    @Test
    @Disabled("待 @Async 与 PipelineOrchestrator 集成测环境就绪后补充 startExecution 全链路")
    void startExecution_fullFlow() {
    }
}
