package com.dataflow.ai.domain.dto;

import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换上下文
 * 提供转换处理器执行所需的上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformContext {

    /**
     * 当前转换节点
     */
    private Transform transform;

    /**
     * 共享状态（在多个转换节点之间共享）
     */
    @Builder.Default
    private Map<String, Object> sharedState = new HashMap<>();

    /**
     * 节点级状态（当前节点的私有状态）
     */
    @Builder.Default
    private Map<String, Object> nodeState = new HashMap<>();

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * Pipeline ID
     */
    private String pipelineId;

    /**
     * 批次序号
     */
    private int batchNumber;

    /**
     * 是否为第一个批次
     */
    @Builder.Default
    private boolean firstBatch = false;

    /**
     * 是否为最后一个批次
     */
    @Builder.Default
    private boolean lastBatch = false;

    /**
     * 获取共享状态值
     */
    public Object getSharedState(String key) {
        return sharedState.get(key);
    }

    /**
     * 设置共享状态值
     */
    public void setSharedState(String key, Object value) {
        sharedState.put(key, value);
    }

    /**
     * 移除共享状态值
     */
    public void removeSharedState(String key) {
        sharedState.remove(key);
    }

    /**
     * 检查共享状态是否存在
     */
    public boolean containsSharedState(String key) {
        return sharedState.containsKey(key);
    }

    /**
     * 获取节点状态值
     */
    public Object getNodeState(String key) {
        return nodeState.get(key);
    }

    /**
     * 设置节点状态值
     */
    public void setNodeState(String key, Object value) {
        nodeState.put(key, value);
    }

    /**
     * 移除节点状态值
     */
    public void removeNodeState(String key) {
        nodeState.remove(key);
    }

    /**
     * 检查节点状态是否存在
     */
    public boolean containsNodeState(String key) {
        return nodeState.containsKey(key);
    }

    /**
     * 获取转换配置中的值
     */
    public Object getConfigValue(String key) {
        if (transform == null || transform.getConfig() == null) {
            return null;
        }
        return transform.getConfig().get(key);
    }

    /**
     * 获取转换配置中的值，带默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfigValue(String key, T defaultValue) {
        Object value = getConfigValue(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * 创建子上下文（用于嵌套转换）
     */
    public TransformContext createChildContext(Transform childTransform) {
        return TransformContext.builder()
                .transform(childTransform)
                .sharedState(this.sharedState)  // 共享同一状态
                .nodeState(new HashMap<>())     // 新的节点状态
                .executionId(this.executionId)
                .pipelineId(this.pipelineId)
                .batchNumber(this.batchNumber)
                .firstBatch(this.firstBatch)
                .lastBatch(this.lastBatch)
                .build();
    }

    /**
     * 创建下一个批次的上下文
     */
    public TransformContext nextBatchContext() {
        return TransformContext.builder()
                .transform(this.transform)
                .sharedState(this.sharedState)
                .nodeState(new HashMap<>())
                .executionId(this.executionId)
                .pipelineId(this.pipelineId)
                .batchNumber(this.batchNumber + 1)
                .firstBatch(false)
                .lastBatch(false)
                .build();
    }

    /**
     * 标记为最后一个批次
     */
    public void markAsLastBatch() {
        this.lastBatch = true;
    }

    /**
     * 清空节点状态
     */
    public void clearNodeState() {
        nodeState.clear();
    }

    /**
     * 获取所有节点状态
     */
    public Map<String, Object> getAllNodeState() {
        return new HashMap<>(nodeState);
    }
}
