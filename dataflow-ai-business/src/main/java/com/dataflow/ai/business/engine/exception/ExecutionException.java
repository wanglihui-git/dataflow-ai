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

    /**
     * 仅包含错误消息的构造器（无执行上下文）。
     *
     * @param message 错误描述
     */
    public ExecutionException(String message) {
        super(message);
        this.executionId = null;
        this.pipelineId = null;
        this.errorCode = "EXECUTION_ERROR";
    }

    /**
     * 包含错误消息与根因的构造器（无执行上下文）。
     *
     * @param message 错误描述
     * @param cause   根因异常
     */
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.executionId = null;
        this.pipelineId = null;
        this.errorCode = "EXECUTION_ERROR";
    }

    /**
     * 绑定执行与 Pipeline 标识的构造器。
     *
     * @param executionId 执行运行 ID
     * @param pipelineId Pipeline ID
     * @param message     错误描述
     */
    public ExecutionException(String executionId, String pipelineId, String message) {
        super(message);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = "EXECUTION_ERROR";
    }

    /**
     * 绑定执行上下文并携带根因的构造器。
     *
     * @param executionId 执行运行 ID
     * @param pipelineId  Pipeline ID
     * @param message       错误描述
     * @param cause         根因异常
     */
    public ExecutionException(String executionId, String pipelineId, String message, Throwable cause) {
        super(message, cause);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = "EXECUTION_ERROR";
    }

    /**
     * 绑定执行上下文并指定业务错误码的构造器。
     *
     * @param executionId 执行运行 ID
     * @param pipelineId  Pipeline ID
     * @param errorCode   业务错误码
     * @param message     错误描述
     */
    public ExecutionException(String executionId, String pipelineId, String errorCode, String message) {
        super(message);
        this.executionId = executionId;
        this.pipelineId = pipelineId;
        this.errorCode = errorCode;
    }

    /**
     * 完整上下文的构造器（含错误码与根因）。
     *
     * @param executionId 执行运行 ID
     * @param pipelineId  Pipeline ID
     * @param errorCode   业务错误码
     * @param message     错误描述
     * @param cause       根因异常
     */
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
        // 步骤1：拼接错误码前缀
        if (errorCode != null) {
            sb.append("[").append(errorCode).append("] ");
        }
        // 步骤2：附加执行 ID 与 Pipeline ID
        if (executionId != null) {
            sb.append("Execution: ").append(executionId);
        }
        if (pipelineId != null) {
            if (executionId != null) sb.append(", ");
            sb.append("Pipeline: ").append(pipelineId);
        }
        // 步骤3：拼接原始异常消息
        if (executionId != null || pipelineId != null) {
            sb.append(" - ");
        }
        sb.append(getMessage());
        return sb.toString();
    }
}
