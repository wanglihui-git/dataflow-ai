package com.dataflow.ai.business.engine.util;

import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.enums.TransformType;
import com.dataflow.ai.domain.vo.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG 转换共享状态：节点输出与 JOIN 右表数据
 */
public final class TransformDagSupport {

    public static final String JOIN_RIGHT_RECORDS = "join_right_records";
    private static final String OUTPUT_PREFIX = "transform_output_";

    private TransformDagSupport() {
    }

    public static void saveNodeOutput(Map<String, Object> sharedState, String nodeId, List<Record> records) {
        sharedState.put(OUTPUT_PREFIX + nodeId, new ArrayList<>(records));
    }

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

    private static String resolveRightNodeId(Transform transform) {
        if (transform.getConfig() != null && transform.getConfig().get("rightNodeId") != null) {
            return String.valueOf(transform.getConfig().get("rightNodeId"));
        }
        if (transform.getDependsOn() != null && !transform.getDependsOn().isEmpty()) {
            return transform.getDependsOn().get(0);
        }
        return null;
    }

    public static Map<String, Object> newSharedState() {
        return new ConcurrentHashMap<>();
    }
}
