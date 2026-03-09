package com.dataflow.ai.domain.enums;

import lombok.Getter;

/**
 * 执行状态枚举
 */
@Getter
public enum ExecutionStatus {
    /**
     * 等待中
     */
    PENDING("等待中"),

    /**
     * 运行中
     */
    RUNNING("运行中"),

    /**
     * 成功
     */
    SUCCESS("成功"),

    /**
     * 失败
     */
    FAILED("失败"),

    /**
     * 已取消
     */
    CANCELLED("已取消");

    private final String description;

    ExecutionStatus(String description) {
        this.description = description;
    }
}
