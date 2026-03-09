package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审计日志Repository接口
 */
public interface AuditLogRepository {

    /**
     * 根据ID查询
     */
    Optional<AuditLog> findById(Long id);

    /**
     * 根据用户ID查询
     */
    List<AuditLog> findByUserId(String userId);

    /**
     * 根据操作动作查询
     */
    List<AuditLog> findByAction(String action);

    /**
     * 根据资源类型和ID查询
     */
    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    /**
     * 查询指定时间范围内的日志
     */
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 保存
     */
    AuditLog save(AuditLog auditLog);

    /**
     * 删除
     */
    void deleteById(Long id);

    /**
     * 查询所有
     */
    List<AuditLog> findAll();

    /**
     * 删除指定时间之前的日志
     */
    long deleteBefore(LocalDateTime beforeTime);
}
