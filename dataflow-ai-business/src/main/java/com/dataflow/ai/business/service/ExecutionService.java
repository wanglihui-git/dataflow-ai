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
 * Pipeline 执行服务接口。
 * <p>管理执行记录的创建、异步运行、状态/指标/日志更新及取消协调。</p>
 */
public interface ExecutionService {

    /**
     * 创建一条待执行的执行记录。
     *
     * @param pipelineId  Pipeline ID
     * @param triggeredBy 触发人用户 ID
     * @return 状态为 PENDING 的执行记录
     */
    ExecutionRun createExecutionRun(String pipelineId, String triggeredBy);

    /**
     * 异步启动 Pipeline 编排执行（由 {@code @Async} 在独立线程中运行）。
     *
     * @param runId    执行记录 ID
     * @param pipeline Pipeline 快照配置
     */
    void startExecution(String runId, Pipeline pipeline);

    /**
     * 仅更新执行状态字段。
     *
     * @param runId  执行记录 ID
     * @param status 新状态
     */
    void updateExecutionStatus(String runId, ExecutionStatus status);

    /**
     * 更新执行终态：状态、结束时间、错误信息与指标 JSON。
     *
     * @param runId        执行记录 ID
     * @param status       终态状态
     * @param errorMessage 失败时的错误信息，成功可为 null
     * @param metrics      执行指标 Map，可为 null
     */
    void updateExecutionResult(String runId, ExecutionStatus status, String errorMessage, Map<String, Object> metrics);

    /**
     * 请求取消执行：内存标志 + 数据库 cancelRequested 标记。
     *
     * @param runId 执行记录 ID
     */
    void cancelExecution(String runId);

    /**
     * 按 ID 查询执行记录。
     *
     * @param runId 执行记录 ID
     * @return 执行记录，不存在则为空
     */
    Optional<ExecutionRun> findById(String runId);

    /**
     * 查询指定 Pipeline 的全部执行记录。
     *
     * @param pipelineId Pipeline ID
     * @return 执行记录列表
     */
    List<ExecutionRun> findByPipelineId(String pipelineId);

    /**
     * 查询当前处于 RUNNING 状态的执行记录。
     *
     * @return 运行中的执行列表
     */
    List<ExecutionRun> findRunningExecutions();

    /**
     * 统计指定 Pipeline 的执行次数与成功率。
     *
     * @param pipelineId Pipeline ID
     * @return 含 total、success、failed、successRate 的 Map
     */
    Map<String, Object> getExecutionStats(String pipelineId);

    /**
     * 按状态分页查询执行记录。
     *
     * @param status   执行状态
     * @param pageable 分页参数
     * @return Spring Data 分页结果
     */
    Page<ExecutionRun> findByStatus(ExecutionStatus status, Pageable pageable);

    /**
     * 向执行记录的 JSON 日志中追加一条阶段日志。
     *
     * @param runId   执行记录 ID
     * @param phase   阶段标识（如 INIT、TRANSFORM）
     * @param message 日志正文
     */
    void appendExecutionLog(String runId, String phase, String message);

    /**
     * 判断数据库中是否已标记取消请求。
     *
     * @param runId 执行记录 ID
     * @return 已请求取消返回 true
     */
    boolean isCancelRequested(String runId);
}
