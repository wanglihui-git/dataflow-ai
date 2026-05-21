package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.ExecutionRunRepository;
import com.dataflow.ai.business.repository.jpa.ExecutionRunJpaRepository;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 执行记录Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExecutionRunRepositoryImpl implements ExecutionRunRepository {

    private final ExecutionRunJpaRepository jpaRepository;

    @Override
    public Optional<ExecutionRun> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ExecutionRun> findByPipelineId(String pipelineId) {
        return jpaRepository.findByPipelineId(pipelineId);
    }

    @Override
    public List<ExecutionRun> findByPipelineIdAndStatus(String pipelineId, ExecutionStatus status) {
        return jpaRepository.findByPipelineIdAndStatus(pipelineId, status);
    }

    @Override
    public List<ExecutionRun> findByTriggeredBy(String triggeredBy) {
        return jpaRepository.findByTriggeredBy(triggeredBy);
    }

    @Override
    @Transactional
    public ExecutionRun save(ExecutionRun executionRun) {
        if (executionRun.getId() == null) {
            executionRun.setId(UUID.randomUUID().toString());
        }
        if (executionRun.getCreatedAt() == null) {
            executionRun.setCreatedAt(LocalDateTime.now());
        }
        return jpaRepository.save(executionRun);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<ExecutionRun> findLatestByPipelineId(String pipelineId) {
        return jpaRepository.findLatestByPipelineId(pipelineId);
    }

    @Override
    public long countByPipelineId(String pipelineId) {
        return jpaRepository.countByPipelineId(pipelineId);
    }

    @Override
    public long countByPipelineIdAndStatus(String pipelineId, ExecutionStatus status) {
        return jpaRepository.countByPipelineIdAndStatus(pipelineId, status);
    }

    @Override
    public Page<ExecutionRun> findByStatus(ExecutionStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional
    public void markCancelRequested(String runId) {
        jpaRepository.findById(runId).ifPresent(run -> {
            run.setCancelRequested(true);
            jpaRepository.save(run);
        });
    }
}
