package com.dataflow.ai.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 预览响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponse {

    /**
     * Schema信息
     */
    private Schema schema;

    /**
     * 数据
     */
    private List<Map<String, Object>> data;

    /**
     * 执行日志
     */
    private List<String> executionLog;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schema {
        /**
         * 字段列表
         */
        private List<FieldInfo> fields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldInfo {
        /**
         * 字段名称
         */
        private String name;

        /**
         * 字段类型
         */
        private String type;
    }
}
