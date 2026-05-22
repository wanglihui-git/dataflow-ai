package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.AiHelper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * AI 辅助 Spring Data JPA 仓储
 */
public interface AiHelperJpaRepository extends JpaRepository<AiHelper, String> {

    List<AiHelper> findByCreatedBy(String createdBy);

    List<AiHelper> findByPipelineId(String pipelineId);

    @Query("SELECT h FROM AiHelper h WHERE h.userFeedback IS NULL")
    List<AiHelper> findWithoutFeedback();

    long countByUserFeedback(Integer userFeedback);

    /**
     * 基于 pgvector 余弦距离的向量相似度搜索。
     * 参数 embedding 格式为 "[x1,x2,...]"（pgvector 文字量）。
     */
    @Query(value = """
            SELECT * FROM ai_helpers
            WHERE (embedding <=> CAST(:embedding AS vector)) < :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :lim
            """, nativeQuery = true)
    List<AiHelper> searchByEmbedding(@Param("embedding") String embedding,
                                     @Param("threshold") double threshold,
                                     @Param("lim") int lim);
}
