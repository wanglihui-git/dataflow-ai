package com.dataflow.ai.business.engine.exception;

import com.dataflow.ai.domain.enums.TransformType;

/**
 * 转换异常
 * 在数据转换过程中抛出
 */
public class TransformException extends ExecutionException {

    /**
     * 转换节点ID
     */
    private final String nodeId;

    /**
     * 转换节点名称
     */
    private final String nodeName;

    /**
     * 转换类型
     */
    private final TransformType transformType;

    /**
     * 批次序号
     */
    private final Integer batchNumber;

    /**
     * 记录ID（如果错误发生在处理特定记录时）
     */
    private final String recordId;

    public TransformException(String message) {
        super(message);
        this.nodeId = null;
        this.nodeName = null;
        this.transformType = null;
        this.batchNumber = null;
        this.recordId = null;
    }

    public TransformException(String message, Throwable cause) {
        super(message, cause);
        this.nodeId = null;
        this.nodeName = null;
        this.transformType = null;
        this.batchNumber = null;
        this.recordId = null;
    }

    public TransformException(String executionId, String pipelineId, String nodeId,
                              String nodeName, TransformType transformType,
                              String message) {
        super(executionId, pipelineId, "TRANSFORM_ERROR", message);
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.transformType = transformType;
        this.batchNumber = null;
        this.recordId = null;
    }

    public TransformException(String executionId, String pipelineId, String nodeId,
                              String nodeName, TransformType transformType,
                              String message, Throwable cause) {
        super(executionId, pipelineId, "TRANSFORM_ERROR", message, cause);
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.transformType = transformType;
        this.batchNumber = null;
        this.recordId = null;
    }

    private TransformException(String executionId, String pipelineId, String nodeId,
                                String nodeName, TransformType transformType,
                                Integer batchNumber, String recordId,
                                String message, Throwable cause) {
        super(executionId, pipelineId, "TRANSFORM_ERROR", message, cause);
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.transformType = transformType;
        this.batchNumber = batchNumber;
        this.recordId = recordId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public TransformType getTransformType() {
        return transformType;
    }

    public Integer getBatchNumber() {
        return batchNumber;
    }

    public String getRecordId() {
        return recordId;
    }

    /**
     * 创建带批次信息的异常
     */
    public TransformException withBatch(int batchNumber) {
        return new TransformException(getExecutionId(), getPipelineId(), nodeId,
                nodeName, transformType, batchNumber, null, getMessage(), getCause());
    }

    /**
     * 创建带记录信息的异常
     */
    public TransformException withRecord(String recordId) {
        return new TransformException(getExecutionId(), getPipelineId(), nodeId,
                nodeName, transformType, batchNumber, recordId, getMessage(), getCause());
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TRANSFORM_ERROR] ");
        if (getExecutionId() != null) {
            sb.append("Execution: ").append(getExecutionId());
        }
        if (nodeId != null) {
            sb.append(", Node: ").append(nodeId);
        }
        if (nodeName != null) {
            sb.append(" (").append(nodeName).append(")");
        }
        if (transformType != null) {
            sb.append(", Type: ").append(transformType);
        }
        if (batchNumber != null) {
            sb.append(", Batch: ").append(batchNumber);
        }
        if (recordId != null) {
            sb.append(", Record: ").append(recordId);
        }
        sb.append(" - ").append(getMessage());
        return sb.toString();
    }

    /**
     * 配置错误异常
     */
    public static TransformException configurationError(String executionId, String pipelineId,
                                                       String nodeId, String nodeName,
                                                       TransformType transformType,
                                                       String configError) {
        return new TransformException(executionId, pipelineId, nodeId,
                nodeName, transformType, "Invalid transform configuration: " + configError);
    }

    /**
     * 处理失败异常
     */
    public static TransformException processingFailed(String executionId, String pipelineId,
                                                       String nodeId, String nodeName,
                                                       TransformType transformType,
                                                       String message, Throwable cause) {
        return new TransformException(executionId, pipelineId, nodeId,
                nodeName, transformType, message, cause);
    }

    /**
     * 字段缺失异常
     */
    public static TransformException missingField(String executionId, String pipelineId,
                                                   String nodeId, String nodeName,
                                                   TransformType transformType,
                                                   String fieldName) {
        return new TransformException(executionId, pipelineId, nodeId,
                nodeName, transformType, "Required field not found: " + fieldName);
    }

    /**
     * 类型转换异常
     */
    public static TransformException typeConversionError(String executionId, String pipelineId,
                                                         String nodeId, String nodeName,
                                                         TransformType transformType,
                                                         String fieldName, String expectedType,
                                                         Object actualValue) {
        return new TransformException(executionId, pipelineId, nodeId,
                nodeName, transformType,
                String.format("Type conversion failed for field '%s': expected %s, got %s",
                        fieldName, expectedType,
                        actualValue != null ? actualValue.getClass().getSimpleName() : "null"));
    }
}
