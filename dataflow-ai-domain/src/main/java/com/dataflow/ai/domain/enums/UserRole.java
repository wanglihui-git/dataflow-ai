package com.dataflow.ai.domain.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    /**
     * 系统管理员
     */
    ADMIN("管理员"),

    /**
     * 开发者
     */
    DEVELOPER("开发者"),

    /**
     * 分析师
     */
    ANALYST("分析师"),

    /**
     * 查看者
     */
    VIEWER("查看者");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}
