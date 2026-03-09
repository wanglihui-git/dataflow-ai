package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.AiHelper;

import java.util.List;
import java.util.Optional;

/**
 * AI辅助Repository接口
 */
public interface AiHelperRepository {

    /**
     * 根据ID查询
     */
    Optional<AiHelper> findById(String id);

    /**
     * 根据创建者查询
     */
    List<AiHelper> findByCreatedBy(String createdBy);

    /**
     * 根据Pipeline ID查询
     */
    List<AiHelper> findByPipelineId(String pipelineId);

    /**
     * 查询未反馈的记录
     */
    List<AiHelper> findWithoutFeedback();

    /**
     * 向量搜索相似指令
     */
    List<AiHelper> searchByEmbedding(float[] embedding, double threshold, int limit);

    /**
     * 保存
     */
    AiHelper save(AiHelper aiHelper);

    /**
     * 删除
     */
    void deleteById(String id);

    /**
     * 查询所有
     */
    List<AiHelper> findAll();

    /**
     * 根据反馈类型统计
     */
    long countByUserFeedback(Integer feedback);
}
