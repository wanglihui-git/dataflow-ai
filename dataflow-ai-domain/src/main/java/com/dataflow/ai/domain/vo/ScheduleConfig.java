package com.dataflow.ai.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调度配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConfig {

    /**
     * 调度类型
     */
    private ScheduleType scheduleType;

    /**
     * Cron表达式（当scheduleType为CRON时使用）
     */
    private String cronExpression;

    /**
     * 间隔时间（秒）（当scheduleType为FIXED_RATE或FIXED_DELAY时使用）
     */
    private Long interval;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 失败重试次数
     */
    private Integer retryCount;

    /**
     * 重试间隔（秒）
     */
    private Long retryInterval;

    /**
     * 调度类型枚举
     */
    public enum ScheduleType {
        /**
         * 手动触发
         */
        MANUAL,

        /**
         * 固定速率
         */
        FIXED_RATE,

        /**
         * 固定延迟
         */
        FIXED_DELAY,

        /**
         * Cron表达式
         */
        CRON
    }
}
