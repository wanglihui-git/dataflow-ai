package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口
 */
public interface AuditLogService {

    /**
     * 记录操作日志
     */
    void log(String userId, String action, String resourceType, String resourceId, Map<String, Object> details);

    /**
     * 查询用户操作日志
     */
    List<AuditLog> findByUserId(String userId);

    /**
     * 查询指定时间范围内的日志
     */
    List<AuditLog> findByTimeRange(LocalDateTime start, LocalDateTime end);

    /**
     * 查询资源操作日志
     */
    List<AuditLog> findByResource(String resourceType, String resourceId);

    /**
     * 清理过期日志
     */
    long cleanupExpiredLogs(LocalDateTime beforeTime);
}
