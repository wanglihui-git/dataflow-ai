package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.AuditLogRepository;
import com.dataflow.ai.domain.entity.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 审计日志Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final Map<Long, AuditLog> auditLogs = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<AuditLog> findById(Long id) {
        return Optional.ofNullable(auditLogs.get(id));
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        return auditLogs.values().stream()
                .filter(log -> log.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByAction(String action) {
        return auditLogs.values().stream()
                .filter(log -> log.getAction().equals(action))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId) {
        return auditLogs.values().stream()
                .filter(log -> log.getResourceType().equals(resourceType) && log.getResourceId().equals(resourceId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogs.values().stream()
                .filter(log -> !log.getCreatedAt().isBefore(start) && !log.getCreatedAt().isAfter(end))
                .collect(Collectors.toList());
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(idGenerator.getAndIncrement());
        }
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(LocalDateTime.now());
        }
        auditLogs.put(auditLog.getId(), auditLog);
        return auditLog;
    }

    @Override
    public void deleteById(Long id) {
        auditLogs.remove(id);
    }

    @Override
    public List<AuditLog> findAll() {
        return new ArrayList<>(auditLogs.values());
    }

    @Override
    public long deleteBefore(LocalDateTime beforeTime) {
        List<Long> toDelete = auditLogs.values().stream()
                .filter(log -> log.getCreatedAt().isBefore(beforeTime))
                .map(AuditLog::getId)
                .toList();
        toDelete.forEach(auditLogs::remove);
        return toDelete.size();
    }
}
