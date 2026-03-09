package com.dataflow.ai.domain.enums;

import lombok.Getter;

/**
 * 反馈类型枚举
 */
@Getter
public enum FeedbackType {
    /**
     * 采纳
     */
    ACCEPT("采纳"),

    /**
     * 修改后采纳
     */
    MODIFY("修改后采纳"),

    /**
     * 拒绝
     */
    REJECT("拒绝");

    private final String description;

    FeedbackType(String description) {
        this.description = description;
    }
}
