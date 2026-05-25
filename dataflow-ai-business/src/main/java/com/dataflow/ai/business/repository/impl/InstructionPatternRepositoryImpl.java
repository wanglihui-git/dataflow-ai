package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.InstructionPatternRepository;
import com.dataflow.ai.business.repository.jpa.InstructionPatternJpaRepository;
import com.dataflow.ai.business.util.VectorSimilarityUtils;
import com.dataflow.ai.domain.entity.InstructionPattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 指令模式仓储实现（PostgreSQL）
 */
@Repository
@RequiredArgsConstructor
public class InstructionPatternRepositoryImpl implements InstructionPatternRepository {

    private final InstructionPatternJpaRepository jpaRepository;

    /**
     * 根据指令哈希查询
     */
    @Override
    public Optional<InstructionPattern> findByInstructionHash(String instructionHash) {
        return jpaRepository.findByInstructionHash(instructionHash);
    }

    /**
     * 基于向量嵌入的相似度搜索
     */
    @Override
    public List<InstructionPattern> searchByEmbedding(float[] embedding, double minSimilarity, int limit) {
        // 将相似度阈值转为 pgvector 余弦距离上限
        double maxDistance = VectorSimilarityUtils.toMaxCosineDistance(minSimilarity);
        return jpaRepository.searchByEmbedding(toVectorLiteral(embedding), maxDistance, limit);
    }

    /**
     * 保存实体
     */
    @Override
    @Transactional
    public InstructionPattern save(InstructionPattern pattern) {
        if (pattern.getCreatedAt() == null) {
            pattern.setCreatedAt(LocalDateTime.now());
        }
        pattern.setLastUsedAt(LocalDateTime.now());
        return jpaRepository.save(pattern);
    }

    /** 将 float 数组格式化为 pgvector 字面量 "[x,y,...]" */
    private static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
