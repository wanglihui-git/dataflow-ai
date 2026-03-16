package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.orchestrator.ExecutionResult;
import com.dataflow.ai.business.engine.orchestrator.PipelineOrchestrator;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 执行服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    @Resource
    private ExecutionRunRepository executionRunRepository;

    @Resource
    private PipelineOrchestrator pipelineOrchestrator;

    // 存储正在运行的执行任务（用于取消）
    private final Map<String, ExecutionContext> runningContexts = new ConcurrentHashMap<>();

    // 存储取消标志
    private final Map<String, AtomicBoolean> cancelledFlags = new ConcurrentHashMap<>();

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
        // 获取执行记录
        Optional<ExecutionRun> executionRunOpt = executionRunRepository.findById(runId);
        if (executionRunOpt.isEmpty()) {
            log.error("Execution run not found: runId={}", runId);
            return;
        }

        // 创建取消标志
        AtomicBoolean cancelledFlag = new AtomicBoolean(false);
        cancelledFlags.put(runId, cancelledFlag);

        try {
            updateExecutionStatus(runId, ExecutionStatus.RUNNING);
            log.info("Starting pipeline execution: runId={}, pipelineName={}", runId, pipeline.getName());

            // 创建执行上下文
            ExecutionContext context = ExecutionContext.builder()
                    .runId(runId)
                    .pipeline(pipeline)
                    .executionRun(executionRunOpt.get())
                    .cancelled(false)
                    .startTime(LocalDateTime.now())
                    .build();

            // 存储上下文用于取消操作
            runningContexts.put(runId, context);

            // 使用PipelineOrchestrator执行Pipeline
            ExecutionResult result = pipelineOrchestrator.execute(context);

            // 检查是否被取消
            if (cancelledFlag.get()) {
                updateExecutionResult(runId, ExecutionStatus.CANCELLED, "Execution was cancelled", result.getMetrics());
                log.info("Pipeline execution cancelled: runId={}", runId);
            } else {
                // 更新执行结果
                updateExecutionResult(runId, result.getStatus(), result.getErrorMessage(), result.buildFullMetrics());
                log.info("Pipeline execution completed: runId={}, status={}, recordsProcessed={}, durationMs={}",
                        runId, result.getStatus(), result.getRecordsProcessed(), result.getDurationMs());
            }

        } catch (Exception e) {
            updateExecutionResult(runId, ExecutionStatus.FAILED, e.getMessage(), null);
            log.error("Pipeline execution failed: runId={}, error={}", runId, e.getMessage(), e);
        } finally {
            // 清理资源
            runningContexts.remove(runId);
            cancelledFlags.remove(runId);
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
        // 设置取消标志
        AtomicBoolean cancelledFlag = cancelledFlags.get(runId);
        if (cancelledFlag != null) {
            cancelledFlag.set(true);
        }

        // 标记执行上下文为已取消
        ExecutionContext context = runningContexts.get(runId);
        if (context != null) {
            context.markCancelled();
        }

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
