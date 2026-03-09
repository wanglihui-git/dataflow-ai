package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.request.CreatePipelineRequest;
import com.dataflow.ai.domain.entity.ExecutionRun;
import com.dataflow.ai.domain.entity.Pipeline;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pipeline服务接口
 */
public interface PipelineService {

    /**
     * 根据ID查询Pipeline
     */
    Optional<Pipeline> findById(String id);

    /**
     * 根据所有者ID查询Pipeline列表
     */
    List<Pipeline> findByOwnerId(String ownerId);

    /**
     * 查询用户有权限访问的Pipeline
     */
    List<Pipeline> findByUser(String userId);

    /**
     * 创建Pipeline
     */
    Pipeline createPipeline(CreatePipelineRequest request, String ownerId);

    /**
     * 更新Pipeline
     */
    Pipeline updatePipeline(String id, Pipeline pipeline);

    /**
     * 删除Pipeline
     */
    void deletePipeline(String id);

    /**
     * 执行Pipeline
     */
    ExecutionRun executePipeline(String pipelineId, String triggeredBy);

    /**
     * 取消Pipeline执行
     */
    void cancelExecution(String runId);

    /**
     * 查询Pipeline的执行记录
     */
    List<ExecutionRun> findExecutionRuns(String pipelineId);

    /**
     * 查询执行记录详情
     */
    Optional<ExecutionRun> findExecutionRunById(String runId);

    /**
     * 预览转换结果
     */
    Map<String, Object> previewTransform(Pipeline pipeline, int sampleSize);

    /**
     * 更新Pipeline状态
     */
    void updatePipelineStatus(String id, String status);
}
