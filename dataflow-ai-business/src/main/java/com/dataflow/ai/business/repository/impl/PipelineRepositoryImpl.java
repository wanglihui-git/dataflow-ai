package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.PipelineRepository;
import com.dataflow.ai.domain.entity.Pipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Pipeline Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class PipelineRepositoryImpl implements PipelineRepository {

    private final Map<String, Pipeline> pipelines = new ConcurrentHashMap<>();

    @Override
    public Optional<Pipeline> findById(String id) {
        return Optional.ofNullable(pipelines.get(id));
    }

    @Override
    public List<Pipeline> findByOwnerId(String ownerId) {
        return pipelines.values().stream()
                .filter(p -> p.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Pipeline> findByPermissionLevel(Pipeline.PermissionLevel permissionLevel) {
        return pipelines.values().stream()
                .filter(p -> p.getPermissionLevel() == permissionLevel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Pipeline> findByUser(String userId) {
        // TODO: 实现基于用户权限的Pipeline查询
        return new ArrayList<>();
    }

    @Override
    public List<Pipeline> findAll() {
        return new ArrayList<>(pipelines.values());
    }

    @Override
    public Pipeline save(Pipeline pipeline) {
        if (pipeline.getId() == null) {
            pipeline.setId(UUID.randomUUID().toString());
        }
        if (pipeline.getCreatedAt() == null) {
            pipeline.setCreatedAt(LocalDateTime.now());
        }
        pipeline.setUpdatedAt(LocalDateTime.now());
        pipelines.put(pipeline.getId(), pipeline);
        return pipeline;
    }

    @Override
    public void deleteById(String id) {
        pipelines.remove(id);
    }

    @Override
    public Optional<Pipeline> findByName(String name) {
        return pipelines.values().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    @Override
    public List<Pipeline> findByStatus(String status) {
        return pipelines.values().stream()
                .filter(p -> p.getStatus().equals(status))
                .collect(Collectors.toList());
    }
}
