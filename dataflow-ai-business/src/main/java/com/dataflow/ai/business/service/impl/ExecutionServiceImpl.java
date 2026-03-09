package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.ExecutionRunRepository;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import com.dataflow.ai.domain.entity.Pipeline;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    @Resource
    private ExecutionRunRepository executionRunRepository;

    // 存储正在运行的执行任务（用于取消）
    private final Map<String, Boolean> runningTasks = new ConcurrentHashMap<>();

    @Override
    public ExecutionRun createExecutionRun(String pipelineId, String triggeredBy) {
        ExecutionRun run = ExecutionRun.builder()
                .id(UUID.randomUUID().toString())
                .pipelineId(pipelineId)
                .status(ExecutionStatus.PENDING)
                .startTime(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .createdAt(LocalDateTime.now())
                .build();
        return executionRunRepository.save(run);
    }

    @Override
    @Async
    public void startExecution(String runId, Pipeline pipeline) {
        runningTasks.put(runId, true);
        try {
            updateExecutionStatus(runId, ExecutionStatus.RUNNING);
            log.info("Starting pipeline execution: runId={}, pipelineName={}", runId, pipeline.getName());

            // TODO: 实现实际的Pipeline执行逻辑
            Thread.sleep(1000); // 模拟执行

            // 成功完成
            Map<String, Object> metrics = Map.of(
                    "rowsProcessed", 100,
                    "durationMs", 1000
            );
            updateExecutionResult(runId, ExecutionStatus.SUCCESS, null, metrics);
            log.info("Pipeline execution completed successfully: runId={}", runId);
        } catch (InterruptedException e) {
            updateExecutionStatus(runId, ExecutionStatus.CANCELLED);
            log.warn("Pipeline execution cancelled: runId={}", runId);
        } catch (Exception e) {
            updateExecutionResult(runId, ExecutionStatus.FAILED, e.getMessage(), null);
            log.error("Pipeline execution failed: runId={}", runId, e);
        } finally {
            runningTasks.remove(runId);
        }
    }

    @Override
    public void updateExecutionStatus(String runId, ExecutionStatus status) {
        Optional<ExecutionRun> runOpt = executionRunRepository.findById(runId);
        if (runOpt.isEmpty()) {
            log.warn("Execution run not found: {}", runId);
            return;
        }
        ExecutionRun run = runOpt.get();
        run.setStatus(status);
        executionRunRepository.save(run);
    }

    @Override
    public void updateExecutionResult(String runId, ExecutionStatus status, String errorMessage, Map<String, Object> metrics) {
        Optional<ExecutionRun> runOpt = executionRunRepository.findById(runId);
        if (runOpt.isEmpty()) {
            log.warn("Execution run not found: {}", runId);
            return;
        }
        ExecutionRun run = runOpt.get();
        run.setStatus(status);
        run.setEndTime(LocalDateTime.now());
        run.setErrorMessage(errorMessage);
        run.setMetrics(metrics);
        executionRunRepository.save(run);
    }

    @Override
    public void cancelExecution(String runId) {
        runningTasks.remove(runId);
        updateExecutionStatus(runId, ExecutionStatus.CANCELLED);
        log.info("Pipeline execution cancelled: runId={}", runId);
    }

    @Override
    public Optional<ExecutionRun> findById(String runId) {
        return executionRunRepository.findById(runId);
    }

    @Override
    public List<ExecutionRun> findByPipelineId(String pipelineId) {
        return executionRunRepository.findByPipelineId(pipelineId);
    }

    @Override
    public List<ExecutionRun> findRunningExecutions() {
        return executionRunRepository.findByPipelineIdAndStatus("", ExecutionStatus.RUNNING);
    }

    @Override
    public Map<String, Object> getExecutionStats(String pipelineId) {
        long total = executionRunRepository.countByPipelineId(pipelineId);
        long success = executionRunRepository.countByPipelineIdAndStatus(pipelineId, ExecutionStatus.SUCCESS);
        long failed = executionRunRepository.countByPipelineIdAndStatus(pipelineId, ExecutionStatus.FAILED);

        return Map.of(
                "total", total,
                "success", success,
                "failed", failed,
                "successRate", total > 0 ? (double) success / total : 0.0
        );
    }
}
