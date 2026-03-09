package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.domain.request.CreatePipelineRequest;
import com.dataflow.ai.business.repository.PipelineRepository;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pipeline服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    @Resource
    private PipelineRepository pipelineRepository;

    @Resource
    private ExecutionService executionService;

    @Override
    public Optional<Pipeline> findById(String id) {
        return pipelineRepository.findById(id);
    }

    @Override
    public List<Pipeline> findByOwnerId(String ownerId) {
        return pipelineRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Pipeline> findByUser(String userId) {
        return pipelineRepository.findByUser(userId);
    }

    @Override
    public Pipeline createPipeline(CreatePipelineRequest request, String ownerId) {
        Pipeline pipeline = Pipeline.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .source(request.getSource())
                .transforms(request.getTransforms())
                .sink(request.getSink())
                .schedule(request.getSchedule())
                .ownerId(ownerId)
                .permissionLevel(Pipeline.PermissionLevel.PRIVATE)
                .status("active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return pipelineRepository.save(pipeline);
    }

    @Override
    public Pipeline updatePipeline(String id, Pipeline pipeline) {
        pipeline.setId(id);
        pipeline.setUpdatedAt(LocalDateTime.now());
        return pipelineRepository.save(pipeline);
    }

    @Override
    public void deletePipeline(String id) {
        pipelineRepository.deleteById(id);
    }

    @Override
    public ExecutionRun executePipeline(String pipelineId, String triggeredBy) {
        Optional<Pipeline> pipelineOpt = pipelineRepository.findById(pipelineId);
        if (pipelineOpt.isEmpty()) {
            throw new RuntimeException("Pipeline不存在");
        }
        ExecutionRun run = executionService.createExecutionRun(pipelineId, triggeredBy);
        executionService.startExecution(run.getId(), pipelineOpt.get());
        return run;
    }

    @Override
    public void cancelExecution(String runId) {
        executionService.cancelExecution(runId);
    }

    @Override
    public List<ExecutionRun> findExecutionRuns(String pipelineId) {
        return executionService.findByPipelineId(pipelineId);
    }

    @Override
    public Optional<ExecutionRun> findExecutionRunById(String runId) {
        return executionService.findById(runId);
    }

    @Override
    public Map<String, Object> previewTransform(Pipeline pipeline, int sampleSize) {
        // TODO: 实现实际的转换预览逻辑
        log.info("Previewing transform for pipeline: {}, sampleSize: {}", pipeline.getName(), sampleSize);
        return Map.of();
    }

    @Override
    public void updatePipelineStatus(String id, String status) {
        Optional<Pipeline> pipelineOpt = pipelineRepository.findById(id);
        if (pipelineOpt.isEmpty()) {
            throw new RuntimeException("Pipeline不存在");
        }
        Pipeline pipeline = pipelineOpt.get();
        pipeline.setStatus(status);
        pipeline.setUpdatedAt(LocalDateTime.now());
        pipelineRepository.save(pipeline);
    }
}
