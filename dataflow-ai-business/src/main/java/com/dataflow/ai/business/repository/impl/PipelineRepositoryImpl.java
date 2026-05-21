package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.PipelineRepository;
import com.dataflow.ai.business.repository.jpa.PipelineJpaRepository;
import com.dataflow.ai.domain.entity.Pipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pipeline Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PipelineRepositoryImpl implements PipelineRepository {

    private final PipelineJpaRepository jpaRepository;

    @Override
    public Optional<Pipeline> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Pipeline> findByOwnerId(String ownerId) {
        return jpaRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Pipeline> findByPermissionLevel(Pipeline.PermissionLevel permissionLevel) {
        return jpaRepository.findByPermissionLevel(permissionLevel);
    }

    @Override
    public List<Pipeline> findByUser(String userId, String role, String department) {
        return PipelineAccessHelper.mergeAccessible(
                jpaRepository.findByOwnerId(userId),
                jpaRepository.findByPermissionLevel(Pipeline.PermissionLevel.PUBLIC),
                jpaRepository.findByPermissionLevel(Pipeline.PermissionLevel.SHARED),
                userId, role, department);
    }

    @Override
    public Page<Pipeline> findAccessiblePage(String userId, String role, String department,
                                             String name, Pageable pageable) {
        List<Pipeline> filtered = PipelineAccessHelper.filterByName(
                findByUser(userId, role, department), name);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Pipeline> pageContent = start >= filtered.size() ? List.of() : filtered.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Override
    public List<Pipeline> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    @Transactional
    public Pipeline save(Pipeline pipeline) {
        if (pipeline.getId() == null) {
            pipeline.setId(UUID.randomUUID().toString());
        }
        if (pipeline.getCreatedAt() == null) {
            pipeline.setCreatedAt(LocalDateTime.now());
        }
        pipeline.setUpdatedAt(LocalDateTime.now());
        return jpaRepository.save(pipeline);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<Pipeline> findByName(String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    public List<Pipeline> findByStatus(String status) {
        return jpaRepository.findByStatus(status);
    }
}
