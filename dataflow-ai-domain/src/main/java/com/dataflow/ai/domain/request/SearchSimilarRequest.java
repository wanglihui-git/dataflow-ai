package com.dataflow.ai.domain.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索相似指令请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSimilarRequest {

    /**
     * 指令文本
     */
    private String instruction;

    /**
     * 限制返回数量
     */
    @Builder.Default
    private int limit = 5;

    /**
     * 最小相似度阈值
     */
    @Builder.Default
    private double minSimilarity = 0.8;
}
