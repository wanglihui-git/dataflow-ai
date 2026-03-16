package com.dataflow.ai.business.engine.exception;

import lombok.Getter;

/**
 * 执行异常基类
 * 所有 Pipeline 执行相关的异常都继承此类
 */
@Getter
public class ExecutionException extends RuntimeException {

    /**
     * 执行ID
     */
    private final String executionId;

    /**
     * Pipeline ID
     */
    private final String pipelineId;

    /**
     * 错误代码
     */
    private final String errorCode;

    public ExecutionException(String message) {
        super(message);
        this.executionId = null;
        this.pipelineId = null;
        this.errorCode = "EXECUTION_ERROR";
    }

    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.executionId = null;
        this.pipelineId = null;
        this.errorCode = "EXECUTION_ERROR";
    }

    public ExecutionException(String executionId, String pipelineId, String message) {
        super(message);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = "EXECUTION_ERROR";
    }

    public ExecutionException(String executionId, String pipelineId, String message, Throwable cause) {
        super(message, cause);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = "EXECUTION_ERROR";
    }

    public ExecutionException(String executionId, String pipelineId, String errorCode, String message) {
        super(message);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = errorCode;
    }

    public ExecutionException(String executionId, String pipelineId, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = errorCode;
    }

    /**
     * 获取详细的错误信息（包含执行上下文）
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        if (errorCode != null) {
            sb.append("[").append(errorCode).append("] ");
        }
        if (executionId != null) {
            sb.append("Execution: ").append(executionId);
        }
        if (pipelineId != null) {
            if (executionId != null) sb.append(", ");
            sb.append("Pipeline: ").append(pipelineId);
        }
        if (executionId != null || pipelineId != null) {
            sb.append(" - ");
        }
        sb.append(getMessage());
        return sb.toString();
    }
}
