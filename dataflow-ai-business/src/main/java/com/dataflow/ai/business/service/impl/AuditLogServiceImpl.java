package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AuditLogRepository;
import com.dataflow.ai.business.service.AuditLogService;
import com.dataflow.ai.domain.entity.AuditLog;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.dataflow.ai.domain.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
        log(userId, action, resourceType, resourceId, details, null, null);
    }

    @Override
    public void log(String userId, String action, String resourceType, String resourceId,
                    Map<String, Object> details, String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log created: userId={}, action={}, resourceType={}, resourceId={}",
                userId, action, resourceType, resourceId);
    }

    @Override
    public PageResponse<AuditLog> findPage(String userId, String action,
                                           LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Stream<AuditLog> stream = auditLogRepository.findAll().stream();
        if (userId != null && !userId.isBlank()) {
            stream = stream.filter(a -> userId.equals(a.getUserId()));
        }
        if (action != null && !action.isBlank()) {
            stream = stream.filter(a -> action.equals(a.getAction()));
        }
        if (start != null) {
            stream = stream.filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isBefore(start));
        }
        if (end != null) {
            stream = stream.filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isAfter(end));
        }
        List<AuditLog> sorted = stream
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), sorted.size());
        List<AuditLog> page = from >= sorted.size() ? List.of() : sorted.subList(from, to);
        return PageResponse.of(page, pageable.getPageNumber(), pageable.getPageSize(), sorted.size());
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
