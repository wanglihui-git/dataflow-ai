package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.AuditLogRepository;
import com.dataflow.ai.business.repository.jpa.AuditLogJpaRepository;
import com.dataflow.ai.domain.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审计日志Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    @Override
    public Optional<AuditLog> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> findByAction(String action) {
        return jpaRepository.findByAction(action);
    }

    @Override
    public List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId) {
        return jpaRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end);
    }

    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(LocalDateTime.now());
        }
        return jpaRepository.save(auditLog);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<AuditLog> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    @Transactional
    public long deleteBefore(LocalDateTime beforeTime) {
        return jpaRepository.deleteByCreatedAtBefore(beforeTime);
    }
}
