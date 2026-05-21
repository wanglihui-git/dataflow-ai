package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.enums.ExecutionStatus;
import com.dataflow.ai.domain.entity.Pipeline;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 执行服务接口
 */
public interface ExecutionService {

    /**
     * 创建执行记录
     */
    ExecutionRun createExecutionRun(String pipelineId, String triggeredBy);

    /**
     * 启动Pipeline执行
     */
    void startExecution(String runId, Pipeline pipeline);

    /**
     * 更新执行状态
     */
    void updateExecutionStatus(String runId, ExecutionStatus status);

    /**
     * 更新执行结果
     */
    void updateExecutionResult(String runId, ExecutionStatus status, String errorMessage, Map<String, Object> metrics);

    /**
     * 取消执行
     */
    void cancelExecution(String runId);

    /**
     * 查询执行记录
     */
    Optional<ExecutionRun> findById(String runId);

    /**
     * 查询Pipeline的执行记录
     */
    List<ExecutionRun> findByPipelineId(String pipelineId);

    /**
     * 查询正在运行的执行记录
     */
    List<ExecutionRun> findRunningExecutions();

    /**
     * 获取执行统计信息
     */
    Map<String, Object> getExecutionStats(String pipelineId);

    /**
     * 按状态分页查询执行记录
     */
    Page<ExecutionRun> findByStatus(ExecutionStatus status, Pageable pageable);

    /**
     * 持久化执行日志条目
     */
    void appendExecutionLog(String runId, String phase, String message);

    /**
     * 是否已请求取消（DB 标志）
     */
    boolean isCancelRequested(String runId);
}
