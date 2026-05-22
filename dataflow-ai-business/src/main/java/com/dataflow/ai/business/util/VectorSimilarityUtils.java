package com.dataflow.ai.business.util;

/**
 * pgvector 余弦距离（{@code <=>}）与余弦相似度换算工具。
 * <p>对 L2 归一化向量：{@code distance = 1 - similarity}。</p>
 */
public final class VectorSimilarityUtils {

    private VectorSimilarityUtils() {
    }

    /**
     * 将最小相似度阈值转为 SQL/pgvector 可用的最大余弦距离。
     *
     * @param minSimilarity 相似度下限，自动裁剪到 [0, 1]
     * @return 对应的最大余弦距离（1 - similarity）
     */
    public static double toMaxCosineDistance(double minSimilarity) {
        double similarity = Math.max(0.0, Math.min(1.0, minSimilarity));
        return 1.0 - similarity;
    }

    /**
     * 将 pgvector 查询返回的余弦距离转为相似度。
     *
     * @param cosineDistance 余弦距离
     * @return 相似度（1 - distance）
     */
    public static double distanceToSimilarity(double cosineDistance) {
        return 1.0 - cosineDistance;
    }
}
