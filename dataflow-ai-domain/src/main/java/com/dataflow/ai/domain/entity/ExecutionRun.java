package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 执行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRun {

    /**
     * 执行记录ID
     */
    private String id;

    /**
     * Pipeline ID
     */
    private String pipelineId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行日志
     */
    private Map<String, Object> executionLog;

    /**
     * 指标数据
     */
    private Map<String, Object> metrics;

    /**
     * 触发者
     */
    private String triggeredBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
