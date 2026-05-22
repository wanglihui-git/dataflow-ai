package com.dataflow.ai.api.controller;

import com.dataflow.ai.business.service.AuditLogService;
import com.dataflow.ai.domain.entity.AuditLog;
import com.dataflow.ai.domain.response.ApiResponse;
import com.dataflow.ai.domain.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 审计日志 REST 控制器。
 * <p>
 * 仅 {@code ROLE_ADMIN} 可访问，支持按用户、动作与时间范围分页查询审计记录。
 * </p>
 */
@RestController
@RequestMapping("/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志查询")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 分页查询审计日志。
     *
     * @param userId 按用户 ID 过滤（可选）
     * @param action 按动作类型过滤（可选）
     * @param start  开始时间 ISO-8601（可选）
     * @param end    结束时间 ISO-8601（可选）
     * @param page   页码
     * @param size   每页条数
     * @return 分页的 {@link AuditLog} 列表
     */
    @GetMapping
    @Operation(summary = "分页查询审计日志")
    public ApiResponse<PageResponse<AuditLog>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ofSuccess(auditLogService.findPage(userId, action, start, end, PageRequest.of(page, size)));
    }
}
