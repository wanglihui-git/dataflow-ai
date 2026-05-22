package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志 Spring Data JPA 仓储
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :beforeTime")
    int deleteByCreatedAtBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
