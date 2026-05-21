package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.engine.preview.PipelinePreviewExecutor;
import com.dataflow.ai.business.repository.PipelineRepository;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.service.PipelineService;
import com.dataflow.ai.business.service.UserService;
import com.dataflow.ai.domain.response.PageResponse;
import org.springframework.data.domain.Page;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.request.CreatePipelineRequest;
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

    @Resource
    private PipelinePreviewExecutor pipelinePreviewExecutor;

    @Resource
    private UserService userService;

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
        return userService.findById(userId)
                .map(u -> pipelineRepository.findByUser(userId, u.getRole().name(), u.getDepartment()))
                .orElse(List.of());
    }

    @Override
    public PageResponse<Pipeline> findByUserPage(String userId, String name, org.springframework.data.domain.Pageable pageable) {
        return userService.findById(userId)
                .map(u -> {
                    Page<Pipeline> page = pipelineRepository.findAccessiblePage(
                            userId, u.getRole().name(), u.getDepartment(), name, pageable);
                    return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
                })
                .orElse(PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0));
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
                .permissionLevel(resolvePermissionLevel(request.getPermissionLevel()))
                .allowedRoles(request.getAllowedRoles())
                .allowedUsers(request.getAllowedUsers())
                .allowedDepartments(request.getAllowedDepartments())
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
        if (sampleSize <= 0) {
            sampleSize = 10;
        }
        log.info("Previewing transform for pipeline: {}, sampleSize: {}", pipeline.getName(), sampleSize);
        try {
            return pipelinePreviewExecutor.preview(pipeline, sampleSize);
        } catch (Exception e) {
            log.error("Pipeline preview failed: {}", e.getMessage(), e);
            throw new RuntimeException("Pipeline 预览失败: " + e.getMessage(), e);
        }
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

    private Pipeline.PermissionLevel resolvePermissionLevel(String level) {
        if (level == null || level.isBlank()) {
            return Pipeline.PermissionLevel.PRIVATE;
        }
        try {
            return Pipeline.PermissionLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Pipeline.PermissionLevel.PRIVATE;
        }
    }
}
