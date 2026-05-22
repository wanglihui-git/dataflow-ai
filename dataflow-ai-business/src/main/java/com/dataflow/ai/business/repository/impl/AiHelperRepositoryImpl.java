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

    /**
     * 根据 ID 查询
     */
    @Override
    public Optional<AiHelper> findById(String id) {
        return jpaRepository.findById(id);
    }

    /**
     * 根据创建人查询
     */
    @Override
    public List<AiHelper> findByCreatedBy(String createdBy) {
        return jpaRepository.findByCreatedBy(createdBy);
    }

    /**
     * 根据 Pipeline ID 查询
     */
    @Override
    public List<AiHelper> findByPipelineId(String pipelineId) {
        return jpaRepository.findByPipelineId(pipelineId);
    }

    /**
     * 查询尚未反馈的 AI 辅助记录
     */
    @Override
    public List<AiHelper> findWithoutFeedback() {
        return jpaRepository.findWithoutFeedback();
    }

    /**
     * 基于向量嵌入的相似度搜索
     */
    @Override
    public List<AiHelper> searchByEmbedding(float[] embedding, double maxCosineDistance, int limit) {
        String vectorLiteral = toVectorLiteral(embedding);
        return jpaRepository.searchByEmbedding(vectorLiteral, maxCosineDistance, limit);
    }

    /**
     * 保存实体
     */
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

    /**
     * 根据 ID 删除
     */
    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    /**
     * 查询全部
     */
    @Override
    public List<AiHelper> findAll() {
        return jpaRepository.findAll();
    }

    /**
     * 按用户反馈统计数量
     */
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
