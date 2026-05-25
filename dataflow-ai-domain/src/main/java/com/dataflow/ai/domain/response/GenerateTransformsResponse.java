package com.dataflow.ai.domain.response;

import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 生成转换节点响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTransformsResponse {

    /**
     * AI 辅助记录 ID（用于 /v1/ai/feedback）
     */
    private String aiHelperId;

    /**
     * 来源信息
     */
    private SourceInfo source;

    /**
     * 生成的转换节点
     */
    private List<Transform> nodes;

    /**
     * 建议信息
     */
    private List<Suggestion> suggestions;

    /**
     * 可视化信息
     */
    private Visualization visualization;

    /**
     * 元数据
     */
    private Metadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {
        /**
         * 来源类型（historical_pattern-历史模式, llm_generated-大模型生成）
         */
        private String type;

        /**
         * 置信度
         */
        private double confidence;

        /**
         * 匹配的历史指令
         */
        private String matchedInstruction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        /**
         * 建议类型（info-信息, warning-警告）
         */
        private String type;

        /**
         * 建议消息
         */
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Visualization {
        /**
         * 摘要
         */
        private String summary;

        /**
         * 数据流向
         */
        private String dataFlow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        /**
         * 处理耗时（毫秒）
         */
        private long processingTimeMs;

        /**
         * 使用的模型
         */
        private String modelUsed;
    }
}
