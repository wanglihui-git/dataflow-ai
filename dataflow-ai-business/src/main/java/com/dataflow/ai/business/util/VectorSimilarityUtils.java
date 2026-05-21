package com.dataflow.ai.business.util;

/**
 * pgvector 余弦距离（{@code <=>}）与余弦相似度换算。
 * 对 L2 归一化向量：distance = 1 - similarity。
 */
public final class VectorSimilarityUtils {

    private VectorSimilarityUtils() {
    }

    /**
     * 将最小相似度阈值转为 SQL 可用的最大余弦距离。
     */
    public static double toMaxCosineDistance(double minSimilarity) {
        double similarity = Math.max(0.0, Math.min(1.0, minSimilarity));
        return 1.0 - similarity;
    }

    /**
     * 将 pgvector 余弦距离转为相似度。
     */
    public static double distanceToSimilarity(double cosineDistance) {
        return 1.0 - cosineDistance;
    }
}
