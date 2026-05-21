package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.business.repository.jpa.AiHelperJpaRepository;
import com.dataflow.ai.business.util.VectorSimilarityUtils;
import com.dataflow.ai.domain.entity.AiHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI辅助Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AiHelperRepositoryImpl implements AiHelperRepository {

    private final AiHelperJpaRepository jpaRepository;

    @Override
    public Optional<AiHelper> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<AiHelper> findByCreatedBy(String createdBy) {
        return jpaRepository.findByCreatedBy(createdBy);
    }

    @Override
    public List<AiHelper> findByPipelineId(String pipelineId) {
        return jpaRepository.findByPipelineId(pipelineId);
    }

    @Override
    public List<AiHelper> findWithoutFeedback() {
        return jpaRepository.findWithoutFeedback();
    }

    @Override
    public List<AiHelper> searchByEmbedding(float[] embedding, double maxCosineDistance, int limit) {
        String vectorLiteral = toVectorLiteral(embedding);
        return jpaRepository.searchByEmbedding(vectorLiteral, maxCosineDistance, limit);
    }

    @Override
    @Transactional
    public AiHelper save(AiHelper aiHelper) {
        if (aiHelper.getId() == null) {
            aiHelper.setId(UUID.randomUUID().toString());
        }
        if (aiHelper.getCreatedAt() == null) {
            aiHelper.setCreatedAt(LocalDateTime.now());
        }
        return jpaRepository.save(aiHelper);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<AiHelper> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public long countByUserFeedback(Integer feedback) {
        return jpaRepository.countByUserFeedback(feedback);
    }

    /**
     * 将 float[] 转换为 pgvector 文字量格式，例如 "[1.0,2.0,3.0]"
     */
    private String toVectorLiteral(float[] embedding) {
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
