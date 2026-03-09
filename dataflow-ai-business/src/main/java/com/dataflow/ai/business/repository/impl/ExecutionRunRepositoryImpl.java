package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.ExecutionRunRepository;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 执行记录Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class ExecutionRunRepositoryImpl implements ExecutionRunRepository {

    private final Map<String, ExecutionRun> executionRuns = new ConcurrentHashMap<>();

    @Override
    public Optional<ExecutionRun> findById(String id) {
        return Optional.ofNullable(executionRuns.get(id));
    }

    @Override
    public List<ExecutionRun> findByPipelineId(String pipelineId) {
        return executionRuns.values().stream()
                .filter(r -> r.getPipelineId().equals(pipelineId))
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionRun> findByPipelineIdAndStatus(String pipelineId, ExecutionStatus status) {
        return executionRuns.values().stream()
                .filter(r -> r.getPipelineId().equals(pipelineId) && r.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionRun> findByTriggeredBy(String triggeredBy) {
        return executionRuns.values().stream()
                .filter(r -> r.getTriggeredBy().equals(triggeredBy))
                .collect(Collectors.toList());
    }

    @Override
    public ExecutionRun save(ExecutionRun executionRun) {
        if (executionRun.getId() == null) {
            executionRun.setId(UUID.randomUUID().toString());
        }
        if (executionRun.getCreatedAt() == null) {
            executionRun.setCreatedAt(LocalDateTime.now());
        }
        executionRuns.put(executionRun.getId(), executionRun);
        return executionRun;
    }

    @Override
    public void deleteById(String id) {
        executionRuns.remove(id);
    }

    @Override
    public Optional<ExecutionRun> findLatestByPipelineId(String pipelineId) {
        return executionRuns.values().stream()
                .filter(r -> r.getPipelineId().equals(pipelineId))
                .max(Comparator.comparing(ExecutionRun::getStartTime));
    }

    @Override
    public long countByPipelineId(String pipelineId) {
        return executionRuns.values().stream()
                .filter(r -> r.getPipelineId().equals(pipelineId))
                .count();
    }

    @Override
    public long countByPipelineIdAndStatus(String pipelineId, ExecutionStatus status) {
        return executionRuns.values().stream()
                .filter(r -> r.getPipelineId().equals(pipelineId) && r.getStatus() == status)
                .count();
    }
}
