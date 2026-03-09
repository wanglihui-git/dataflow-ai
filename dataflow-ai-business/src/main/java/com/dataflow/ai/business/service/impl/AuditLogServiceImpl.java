package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AuditLogRepository;
import com.dataflow.ai.business.service.AuditLogService;
import com.dataflow.ai.domain.entity.AuditLog;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private AuditLogRepository auditLogRepository;

    @Override
    public void log(String userId, String action, String resourceType, String resourceId, Map<String, Object> details) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log created: userId={}, action={}, resourceType={}, resourceId={}",
                userId, action, resourceType, resourceId);
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> findByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByCreatedAtBetween(start, end);
    }

    @Override
    public List<AuditLog> findByResource(String resourceType, String resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public long cleanupExpiredLogs(LocalDateTime beforeTime) {
        log.info("Cleaning up audit logs before: {}", beforeTime);
        return auditLogRepository.deleteBefore(beforeTime);
    }
}
