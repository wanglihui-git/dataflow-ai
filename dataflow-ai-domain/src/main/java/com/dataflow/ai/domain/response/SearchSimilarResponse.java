package com.dataflow.ai.domain.response;

import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索相似指令响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSimilarResponse {

    /**
     * 搜索结果列表
     */
    private List<SimilarResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarResult {
        /**
         * 指令文本
         */
        private String instruction;

        /**
         * 相似度
         */
        private double similarity;

        /**
         * 使用次数
         */
        private int useCount;

        /**
         * 采纳率
         */
        private double acceptanceRate;

        /**
         * 生成的节点
         */
        private List<Transform> generatedNodes;
    }
}
