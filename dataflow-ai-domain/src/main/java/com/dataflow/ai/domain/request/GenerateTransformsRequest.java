package com.dataflow.ai.domain.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 生成转换节点请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTransformsRequest {

    /**
     * 自然语言指令
     */
    private String instruction;

    /**
     * 关联 Pipeline ID（可选）
     */
    private String pipelineId;

    /**
     * 上下文信息
     */
    private Context context;

    /**
     * 选项
     */
    private Options options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        /**
         * 源数据schema
         */
        private SourceSchema sourceSchema;

        /**
         * 目标schema（可选）
         */
        private TargetSchema targetSchema;

        /**
         * 样本数据（可选）
         */
        private List<Map<String, Object>> sampleData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceSchema {
        /**
         * 字段列表
         */
        private List<FieldInfo> fields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetSchema {
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

        /**
         * 样本值
         */
        private Object sample;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {
        /**
         * 最大节点数
         */
        @Builder.Default
        private int maxNodes = 10;

        /**
         * 是否严格模式
         */
        @Builder.Default
        private boolean strict = true;
    }
}
