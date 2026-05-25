package com.dataflow.ai.business.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * VectorSimilarityUtils 余弦相似度与阈值校正。
 */

class VectorSimilarityUtilsTest {

    /**
     * 验证：toMaxCosineDistance - minSimilarity 0.8 -> maxDistance 0.2。
     */
    @Test
    @DisplayName("toMaxCosineDistance - minSimilarity 0.8 -> maxDistance 0.2")
    void toMaxCosineDistance() {
        // 断言：校验响应或交互
        assertEquals(0.2, VectorSimilarityUtils.toMaxCosineDistance(0.8), 0.0001);
    }

    /**
     * 验证：distanceToSimilarity。
     */
    @Test
    @DisplayName("distanceToSimilarity")
    void distanceToSimilarity() {
        // 断言：校验响应或交互
        assertEquals(0.8, VectorSimilarityUtils.distanceToSimilarity(0.2), 0.0001);
    }
}
