package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 执行记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "execution_runs")
public class ExecutionRun {

    /**
     * 执行记录ID
     */
    @Id
    private String id;

    /**
     * Pipeline ID
     */
    private String pipelineId;

    /**
     * 执行状态
     */
    @Enumerated(EnumType.STRING)
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> executionLog;

    /**
     * 指标数据
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    /**
     * 触发者
     */
    private String triggeredBy;

    /**
     * 是否请求取消（跨实例可见）
     */
    @Column(name = "cancel_requested")
    private Boolean cancelRequested;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
