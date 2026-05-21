package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.orchestrator.ExecutionResult;
import com.dataflow.ai.business.engine.orchestrator.PipelineOrchestrator;
import com.dataflow.ai.business.repository.ExecutionRunRepository;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.util.ExecutionLogAppender;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionServiceImpl implements ExecutionService {

    @Resource
    private ExecutionRunRepository executionRunRepository;

    @Resource
    private PipelineOrchestrator pipelineOrchestrator;

    private final Map<String, ExecutionContext> runningContexts = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> cancelledFlags = new ConcurrentHashMap<>();

    @Override
    public ExecutionRun createExecutionRun(String pipelineId, String triggeredBy) {
        ExecutionRun run = ExecutionRun.builder()
                .id(UUID.randomUUID().toString())
                .pipelineId(pipelineId)
                .status(ExecutionStatus.PENDING)
                .startTime(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .cancelRequested(false)
                .createdAt(LocalDateTime.now())
                .build();
        return executionRunRepository.save(run);
    }

    @Override
    @Async
    public void startExecution(String runId, Pipeline pipeline) {
        Optional<ExecutionRun> executionRunOpt = executionRunRepository.findById(runId);
        if (executionRunOpt.isEmpty()) {
            log.error("Execution run not found: runId={}", runId);
            return;
        }

        AtomicBoolean cancelledFlag = new AtomicBoolean(false);
        cancelledFlags.put(runId, cancelledFlag);

        try {
            updateExecutionStatus(runId, ExecutionStatus.RUNNING);
            appendExecutionLog(runId, "INIT", "Pipeline execution started: " + pipeline.getName());

            ExecutionContext context = ExecutionContext.builder()
                    .runId(runId)
                    .pipeline(pipeline)
                    .executionRun(executionRunOpt.get())
                    .cancelled(false)
                    .startTime(LocalDateTime.now())
                    .build();

            runningContexts.put(runId, context);

            ExecutionResult result = pipelineOrchestrator.execute(context);

            refreshCancelFlag(runId, cancelledFlag);

            if (cancelledFlag.get() || isCancelRequested(runId)) {
                updateExecutionResult(runId, ExecutionStatus.CANCELLED, "Execution was cancelled", result.getMetrics());
                log.info("Pipeline execution cancelled: runId={}", runId);
            } else {
                updateExecutionResult(runId, result.getStatus(), result.getErrorMessage(), result.buildFullMetrics());
                appendExecutionLog(runId, "COMPLETE", "Status: " + result.getStatus());
                log.info("Pipeline execution completed: runId={}, status={}", runId, result.getStatus());
            }

        } catch (Exception e) {
            updateExecutionResult(runId, ExecutionStatus.FAILED, e.getMessage(), null);
            appendExecutionLog(runId, "ERROR", e.getMessage());
            log.error("Pipeline execution failed: runId={}", runId, e);
        } finally {
            runningContexts.remove(runId);
            cancelledFlags.remove(runId);
        }
    }

    private void refreshCancelFlag(String runId, AtomicBoolean cancelledFlag) {
        if (isCancelRequested(runId)) {
            cancelledFlag.set(true);
            ExecutionContext ctx = runningContexts.get(runId);
            if (ctx != null) {
                ctx.markCancelled();
            }
        }
    }

    @Override
    public void updateExecutionStatus(String runId, ExecutionStatus status) {
        executionRunRepository.findById(runId).ifPresent(run -> {
            run.setStatus(status);
            executionRunRepository.save(run);
        });
    }

    @Override
    public void updateExecutionResult(String runId, ExecutionStatus status, String errorMessage, Map<String, Object> metrics) {
        executionRunRepository.findById(runId).ifPresent(run -> {
            run.setStatus(status);
            run.setEndTime(LocalDateTime.now());
            run.setErrorMessage(errorMessage);
            if (metrics != null) {
                run.setMetrics(metrics);
            }
            executionRunRepository.save(run);
        });
    }

    @Override
    public void cancelExecution(String runId) {
        AtomicBoolean cancelledFlag = cancelledFlags.get(runId);
        if (cancelledFlag != null) {
            cancelledFlag.set(true);
        }
        ExecutionContext context = runningContexts.get(runId);
        if (context != null) {
            context.markCancelled();
        }
        executionRunRepository.markCancelRequested(runId);
        updateExecutionStatus(runId, ExecutionStatus.CANCELLED);
        log.info("Pipeline execution cancel requested: runId={}", runId);
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
        return executionRunRepository.findByStatus(ExecutionStatus.RUNNING, Pageable.unpaged()).getContent();
    }

    @Override
    public Page<ExecutionRun> findByStatus(ExecutionStatus status, Pageable pageable) {
        return executionRunRepository.findByStatus(status, pageable);
    }

    @Override
    public void appendExecutionLog(String runId, String phase, String message) {
        executionRunRepository.findById(runId).ifPresent(run -> {
            ExecutionLogAppender.append(run, phase, message);
            executionRunRepository.save(run);
        });
    }

    @Override
    public boolean isCancelRequested(String runId) {
        return executionRunRepository.findById(runId)
                .map(r -> Boolean.TRUE.equals(r.getCancelRequested()))
                .orElse(false);
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
