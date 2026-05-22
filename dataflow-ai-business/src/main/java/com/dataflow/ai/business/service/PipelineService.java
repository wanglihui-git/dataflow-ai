package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.request.CreatePipelineRequest;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;

import com.dataflow.ai.domain.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pipeline 编排服务接口。
 * <p>管理 Pipeline 生命周期、权限范围内的查询、执行触发与转换预览。</p>
 */
public interface PipelineService {

    /**
     * 按 ID 查询 Pipeline。
     *
     * @param id Pipeline ID
     * @return Pipeline 实体，不存在则为空
     */
    Optional<Pipeline> findById(String id);

    /**
     * 查询指定所有者创建的全部 Pipeline。
     *
     * @param ownerId 所有者用户 ID
     * @return Pipeline 列表
     */
    List<Pipeline> findByOwnerId(String ownerId);

    /**
     * 查询当前用户有权访问的 Pipeline（含角色、部门共享规则）。
     *
     * @param userId 用户 ID
     * @return 可访问的 Pipeline 列表
     */
    List<Pipeline> findByUser(String userId);

    /**
     * 分页查询用户可访问的 Pipeline，支持按名称模糊过滤。
     *
     * @param userId   用户 ID
     * @param name     名称关键字，null 或空表示不过滤
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResponse<Pipeline> findByUserPage(String userId, String name, Pageable pageable);

    /**
     * 创建 Pipeline 并绑定所有者。
     *
     * @param request 创建请求（源、转换、目标、调度等配置）
     * @param ownerId 所有者用户 ID
     * @return 持久化后的 Pipeline
     */
    Pipeline createPipeline(CreatePipelineRequest request, String ownerId);

    /**
     * 更新 Pipeline 配置。
     *
     * @param id       Pipeline ID
     * @param pipeline 更新内容
     * @return 更新后的 Pipeline
     */
    Pipeline updatePipeline(String id, Pipeline pipeline);

    /**
     * 按 ID 删除 Pipeline。
     *
     * @param id Pipeline ID
     */
    void deletePipeline(String id);

    /**
     * 异步触发 Pipeline 执行并返回执行记录。
     *
     * @param pipelineId  Pipeline ID
     * @param triggeredBy 触发人用户 ID
     * @return 新建的执行记录（初始状态为 PENDING）
     */
    ExecutionRun executePipeline(String pipelineId, String triggeredBy);

    /**
     * 请求取消正在进行的执行。
     *
     * @param runId 执行记录 ID
     */
    void cancelExecution(String runId);

    /**
     * 查询 Pipeline 下的历史执行记录。
     *
     * @param pipelineId Pipeline ID
     * @return 执行记录列表
     */
    List<ExecutionRun> findExecutionRuns(String pipelineId);

    /**
     * 按执行 ID 查询单条执行记录。
     *
     * @param runId 执行记录 ID
     * @return 执行记录，不存在则为空
     */
    Optional<ExecutionRun> findExecutionRunById(String runId);

    /**
     * 对 Pipeline 转换链进行采样预览（不写入 Sink）。
     *
     * @param pipeline   Pipeline 配置
     * @param sampleSize 采样行数，≤0 时使用默认值
     * @return 预览结果（含样本行、列信息等）
     */
    Map<String, Object> previewTransform(Pipeline pipeline, int sampleSize);

    /**
     * 更新 Pipeline 状态字段（如 active、disabled）。
     *
     * @param id     Pipeline ID
     * @param status 新状态字符串
     */
    void updatePipelineStatus(String id, String status);
}
