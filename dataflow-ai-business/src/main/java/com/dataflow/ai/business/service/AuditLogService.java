package com.dataflow.ai.business.service;

import com.dataflow.ai.domain.entity.AuditLog;

import com.dataflow.ai.domain.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口。
 * <p>记录用户操作轨迹，支持按用户、时间、资源查询及过期清理。</p>
 */
public interface AuditLogService {

    /**
     * 记录操作日志（不含客户端 IP 与 User-Agent）。
     *
     * @param userId       操作用户 ID
     * @param action       操作类型（如 CREATE、DELETE）
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @param details      扩展详情 JSON 字段
     */
    void log(String userId, String action, String resourceType, String resourceId, Map<String, Object> details);

    /**
     * 记录操作日志（含请求来源信息）。
     *
     * @param userId       操作用户 ID
     * @param action       操作类型
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @param details      扩展详情
     * @param ipAddress    客户端 IP，可为 null
     * @param userAgent    客户端 User-Agent，可为 null
     */
    void log(String userId, String action, String resourceType, String resourceId,
             Map<String, Object> details, String ipAddress, String userAgent);

    /**
     * 多条件分页查询审计日志。
     *
     * @param userId   用户 ID 过滤，null 表示不限
     * @param action   操作类型过滤，null 表示不限
     * @param start    起始时间（含），null 表示不限
     * @param end      结束时间（含），null 表示不限
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResponse<com.dataflow.ai.domain.entity.AuditLog> findPage(
            String userId, String action, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 查询指定用户的全部操作日志。
     *
     * @param userId 用户 ID
     * @return 日志列表
     */
    List<AuditLog> findByUserId(String userId);

    /**
     * 查询指定时间范围内的日志。
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 日志列表
     */
    List<AuditLog> findByTimeRange(LocalDateTime start, LocalDateTime end);

    /**
     * 查询针对某资源的操作日志。
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 日志列表
     */
    List<AuditLog> findByResource(String resourceType, String resourceId);

    /**
     * 删除指定时间之前的过期日志。
     *
     * @param beforeTime 截止时间（不含该时刻之后的数据保留）
     * @return 删除条数
     */
    long cleanupExpiredLogs(LocalDateTime beforeTime);
}
