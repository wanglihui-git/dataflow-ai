package com.dataflow.ai.domain.enums;

import lombok.Getter;

/**
 * 访问类型枚举
 */
@Getter
public enum AccessType {
    /**
     * 无权限
     */
    NONE("无权限"),

    /**
     * 脱敏访问
     */
    MASKED("脱敏访问"),

    /**
     * 完全访问
     */
    FULL("完全访问");

    private final String description;

    AccessType(String description) {
        this.description = description;
    }
}
