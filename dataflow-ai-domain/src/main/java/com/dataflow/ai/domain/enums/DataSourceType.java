package com.dataflow.ai.domain.enums;

import lombok.Getter;

/**
 * 数据源类型枚举
 */
@Getter
public enum DataSourceType {
    /**
     * MySQL数据库
     */
    MYSQL("MySQL"),

    /**
     * PostgreSQL数据库
     */
    POSTGRES("PostgreSQL"),

    /**
     * API接口
     */
    API("API"),

    /**
     * Kafka消息队列
     */
    KAFKA("Kafka"),

    /**
     * CSV文件
     */
    CSV("CSV");

    private final String displayName;

    DataSourceType(String displayName) {
        this.displayName = displayName;
    }
}
