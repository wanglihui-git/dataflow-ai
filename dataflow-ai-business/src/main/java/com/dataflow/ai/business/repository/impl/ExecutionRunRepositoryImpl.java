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

    /**
     * 根据 ID 查询
     */
    @Override
    public Optional<ExecutionRun> findById(String id) {
        return jpaRepository.findById(id);
    }

    /**
     * 根据 Pipeline ID 查询
     */
    @Override
    public List<ExecutionRun> findByPipelineId(String pipelineId) {
        return jpaRepository.findByPipelineId(pipelineId);
    }

    /**
     * 根据 Pipeline ID 与状态查询
     */
    @Override
    public List<ExecutionRun> findByPipelineIdAndStatus(String pipelineId, ExecutionStatus status) {
        return jpaRepository.findByPipelineIdAndStatus(pipelineId, status);
    }

    /**
     * 根据触发人查询
     */
    @Override
    public List<ExecutionRun> findByTriggeredBy(String triggeredBy) {
        return jpaRepository.findByTriggeredBy(triggeredBy);
    }

    /**
     * 保存实体
     */
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

    /**
     * 根据 ID 删除
     */
    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    /**
     * 查询 Pipeline 最近一次执行
     */
    @Override
    public Optional<ExecutionRun> findLatestByPipelineId(String pipelineId) {
        return jpaRepository.findLatestByPipelineId(pipelineId);
    }

    /**
     * 统计 Pipeline 执行次数
     */
    @Override
    public long countByPipelineId(String pipelineId) {
        return jpaRepository.countByPipelineId(pipelineId);
    }

    /**
     * 按状态统计 Pipeline 执行次数
     */
    @Override
    public long countByPipelineIdAndStatus(String pipelineId, ExecutionStatus status) {
        return jpaRepository.countByPipelineIdAndStatus(pipelineId, status);
    }

    /**
     * 根据状态查询
     */
    @Override
    public Page<ExecutionRun> findByStatus(ExecutionStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }

    /**
     * 标记执行记录为已请求取消
     */
    @Override
    @Transactional
    public void markCancelRequested(String runId) {
        jpaRepository.findById(runId).ifPresent(run -> {
            run.setCancelRequested(true);
            jpaRepository.save(run);
        });
    }
}
