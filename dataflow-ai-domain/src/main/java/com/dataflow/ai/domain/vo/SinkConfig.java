package com.dataflow.ai.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 目标配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SinkConfig {

    /**
     * 目标数据源ID
     */
    private String dataSourceId;

    /**
     * 目标表名
     */
    private String tableName;

    /**
     * 写入模式
     */
    private WriteMode writeMode;

    /**
     * 批量大小
     */
    private Integer batchSize;

    /**
     * 其他配置参数
     */
    private Map<String, Object> params;

    /**
     * 写入模式枚举
     */
    public enum WriteMode {
        /**
         * 追加
         */
        APPEND,

        /**
         * 覆盖
         */
        OVERWRITE,

        /**
         * 忽略重复
         */
        IGNORE_DUPLICATES,

        /**
         * 更新现有
         */
        UPDATE_EXISTING
    }
}
