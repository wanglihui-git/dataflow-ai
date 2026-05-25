package com.dataflow.ai.business.engine.util;

import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.domain.vo.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG 转换共享状态工具。
 * <p>在 {@code sharedState} 中保存各节点输出，并为 JOIN 节点注入右表记录列表。</p>
 */
public final class TransformDagSupport {

    /** sharedState 中 JOIN 右表记录的键名。 */
    public static final String JOIN_RIGHT_RECORDS = "join_right_records";
    private static final String OUTPUT_PREFIX = "transform_output_";

    private TransformDagSupport() {
    }

    /**
     * 将节点输出快照写入共享状态，供下游或 JOIN 引用。
     *
     * @param sharedState 执行上下文共享 Map
     * @param nodeId      转换节点 ID
     * @param records     该节点处理后的记录列表（会复制一份存储）
     */
    public static void saveNodeOutput(Map<String, Object> sharedState, String nodeId, List<Record> records) {
        sharedState.put(OUTPUT_PREFIX + nodeId, new ArrayList<>(records));
    }

    /**
     * 若当前节点为 JOIN，从依赖的右节点输出加载右表数据到 {@link #JOIN_RIGHT_RECORDS}。
     *
     * @param transform   当前转换节点配置
     * @param sharedState 执行上下文共享 Map
     */
    public static void prepareJoinRightData(Transform transform, Map<String, Object> sharedState) {
        if (transform.getType() != TransformType.JOIN) {
            return;
        }
        String rightNodeId = resolveRightNodeId(transform);
        if (rightNodeId == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Record> right = (List<Record>) sharedState.get(OUTPUT_PREFIX + rightNodeId);
        if (right != null) {
            sharedState.put(JOIN_RIGHT_RECORDS, right);
        }
    }

    /**
     * 从 config.rightNodeId 或 dependsOn 首项解析 JOIN 右表节点 ID。
     */
    private static String resolveRightNodeId(Transform transform) {
        if (transform.getConfig() != null && transform.getConfig().get("rightNodeId") != null) {
            return String.valueOf(transform.getConfig().get("rightNodeId"));
        }
        if (transform.getDependsOn() != null && !transform.getDependsOn().isEmpty()) {
            return transform.getDependsOn().get(0);
        }
        return null;
    }

    /**
     * 创建线程安全的空共享状态 Map。
     *
     * @return 新的 {@link ConcurrentHashMap} 实例
     */
    public static Map<String, Object> newSharedState() {
        return new ConcurrentHashMap<>();
    }
}
