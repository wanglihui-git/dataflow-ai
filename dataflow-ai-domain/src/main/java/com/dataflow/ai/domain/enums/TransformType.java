package com.dataflow.ai.domain.enums;

import lombok.Getter;

/**
 * 转换类型枚举
 */
@Getter
public enum TransformType {
    /**
     * 字段映射
     */
    FIELD_MAPPER("字段映射"),

    /**
     * 过滤
     */
    FILTER("过滤"),

    /**
     * 扁平化
     */
    FLATTEN("扁平化"),

    /**
     * 查找
     */
    LOOKUP("查找"),

    /**
     * 脚本转换
     */
    SCRIPT("脚本转换"),

    /**
     * AI辅助
     */
    AI_ASSISTED("AI辅助"),

    /**
     * 聚合
     */
    AGGREGATE("聚合"),

    /**
     * 连接
     */
    JOIN("连接"),

    /**
     * 排序
     */
    SORT("排序"),

    /**
     * 分组
     */
    GROUP("分组");

    private final String description;

    TransformType(String description) {
        this.description = description;
    }
}
