package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.AiHelperRepository;
import com.dataflow.ai.domain.entity.AiHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI辅助Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class AiHelperRepositoryImpl implements AiHelperRepository {

    private final Map<String, AiHelper> aiHelpers = new ConcurrentHashMap<>();

    @Override
    public Optional<AiHelper> findById(String id) {
        return Optional.ofNullable(aiHelpers.get(id));
    }

    @Override
    public List<AiHelper> findByCreatedBy(String createdBy) {
        return aiHelpers.values().stream()
                .filter(h -> h.getCreatedBy().equals(createdBy))
                .collect(Collectors.toList());
    }

    @Override
    public List<AiHelper> findByPipelineId(String pipelineId) {
        return aiHelpers.values().stream()
                .filter(h -> h.getPipelineId() != null && h.getPipelineId().equals(pipelineId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AiHelper> findWithoutFeedback() {
        return aiHelpers.values().stream()
                .filter(h -> h.getUserFeedback() == null)
                .collect(Collectors.toList());
    }

    @Override
    public List<AiHelper> searchByEmbedding(float[] embedding, double threshold, int limit) {
        // TODO: 实现向量相似度搜索
        return aiHelpers.values().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public AiHelper save(AiHelper aiHelper) {
        if (aiHelper.getId() == null) {
            aiHelper.setId(UUID.randomUUID().toString());
        }
        if (aiHelper.getCreatedAt() == null) {
            aiHelper.setCreatedAt(LocalDateTime.now());
        }
        aiHelpers.put(aiHelper.getId(), aiHelper);
        return aiHelper;
    }

    @Override
    public void deleteById(String id) {
        aiHelpers.remove(id);
    }

    @Override
    public List<AiHelper> findAll() {
        return new ArrayList<>(aiHelpers.values());
    }

    @Override
    public long countByUserFeedback(Integer feedback) {
        return aiHelpers.values().stream()
                .filter(h -> feedback.equals(h.getUserFeedback()))
                .count();
    }
}
