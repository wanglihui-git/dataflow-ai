package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;

import java.util.List;
import java.util.Optional;

/**
 * 执行记录Repository接口
 */
public interface ExecutionRunRepository {

    /**
     * 根据ID查询执行记录
     */
    Optional<ExecutionRun> findById(String id);

    /**
     * 根据Pipeline ID查询执行记录列表
     */
    List<ExecutionRun> findByPipelineId(String pipelineId);

    /**
     * 根据Pipeline ID和状态查询执行记录
     */
    List<ExecutionRun> findByPipelineIdAndStatus(String pipelineId, ExecutionStatus status);

    /**
     * 查询用户触发的执行记录
     */
    List<ExecutionRun> findByTriggeredBy(String triggeredBy);

    /**
     * 保存执行记录
     */
    ExecutionRun save(ExecutionRun executionRun);

    /**
     * 删除执行记录
     */
    void deleteById(String id);

    /**
     * 查询最新的执行记录
     */
    Optional<ExecutionRun> findLatestByPipelineId(String pipelineId);

    /**
     * 统计Pipeline的执行次数
     */
    long countByPipelineId(String pipelineId);

    /**
     * 统计Pipeline的成功执行次数
     */
    long countByPipelineIdAndStatus(String pipelineId, ExecutionStatus status);
}
