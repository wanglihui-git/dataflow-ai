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

@Repository
@RequiredArgsConstructor
public class InstructionPatternRepositoryImpl implements InstructionPatternRepository {

    private final InstructionPatternJpaRepository jpaRepository;

    @Override
    public Optional<InstructionPattern> findByInstructionHash(String instructionHash) {
        return jpaRepository.findByInstructionHash(instructionHash);
    }

    @Override
    public List<InstructionPattern> searchByEmbedding(float[] embedding, double minSimilarity, int limit) {
        double maxDistance = VectorSimilarityUtils.toMaxCosineDistance(minSimilarity);
        return jpaRepository.searchByEmbedding(toVectorLiteral(embedding), maxDistance, limit);
    }

    @Override
    @Transactional
    public InstructionPattern save(InstructionPattern pattern) {
        if (pattern.getCreatedAt() == null) {
            pattern.setCreatedAt(LocalDateTime.now());
        }
        pattern.setLastUsedAt(LocalDateTime.now());
        return jpaRepository.save(pattern);
    }

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
