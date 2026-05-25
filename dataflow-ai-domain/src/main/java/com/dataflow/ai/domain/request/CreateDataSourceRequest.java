package com.dataflow.ai.domain.request;

import com.dataflow.ai.domain.enums.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建数据源请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDataSourceRequest {

    private String name;

    private DataSourceType type;

    private Map<String, Object> connectionConfig;
}
