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

    /**
     * 根据 ID 查询
     */
    @Override
    public Optional<AuditLog> findById(Long id) {
        return jpaRepository.findById(id);
    }

    /**
     * 根据用户 ID 查询审计日志
     */
    @Override
    public List<AuditLog> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId);
    }

    /**
     * 根据操作类型查询审计日志
     */
    @Override
    public List<AuditLog> findByAction(String action) {
        return jpaRepository.findByAction(action);
    }

    /**
     * 根据资源类型与 ID 查询审计日志
     */
    @Override
    public List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId) {
        return jpaRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    /**
     * 按创建时间范围查询审计日志
     */
    @Override
    public List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end);
    }

    /**
     * 保存实体
     */
    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(LocalDateTime.now());
        }
        return jpaRepository.save(auditLog);
    }

    /**
     * 根据 ID 删除
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    /**
     * 查询全部
     */
    @Override
    public List<AuditLog> findAll() {
        return jpaRepository.findAll();
    }

    /**
     * 删除指定时间之前的审计日志
     */
    @Override
    @Transactional
    public long deleteBefore(LocalDateTime beforeTime) {
        return jpaRepository.deleteByCreatedAtBefore(beforeTime);
    }
}
