package com.dataflow.ai.business.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorSimilarityUtilsTest {

    @Test
    @DisplayName("toMaxCosineDistance - minSimilarity 0.8 -> maxDistance 0.2")
    void toMaxCosineDistance() {
        assertEquals(0.2, VectorSimilarityUtils.toMaxCosineDistance(0.8), 0.0001);
    }

    @Test
    @DisplayName("distanceToSimilarity")
    void distanceToSimilarity() {
        assertEquals(0.8, VectorSimilarityUtils.distanceToSimilarity(0.2), 0.0001);
    }
}
