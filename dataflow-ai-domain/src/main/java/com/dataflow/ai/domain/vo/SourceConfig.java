package com.dataflow.ai.domain.vo;

import com.dataflow.ai.domain.enums.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据源配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceConfig {

    /**
     * 数据源ID（关联data_sources表）
     */
    private String dataSourceId;

    /**
     * 数据源类型
     */
    private DataSourceType type;

    /**
     * 表名（数据库类型数据源）
     */
    private String tableName;

    /**
     * SQL查询语句（数据库类型数据源）
     */
    private String query;

    /**
     * 其他配置参数
     */
    private Map<String, Object> params;
}
