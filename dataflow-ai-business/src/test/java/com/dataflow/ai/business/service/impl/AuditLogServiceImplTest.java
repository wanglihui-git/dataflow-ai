package com.dataflow.ai.business.service.impl;

import com.dataflow.ai.business.repository.AuditLogRepository;
import com.dataflow.ai.domain.entity.AuditLog;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditLogServiceImpl 分页查询单测。
 */

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    /**
     * 验证：log - 写入审计记录。
     */
    @Test
    @DisplayName("log - 写入审计记录")
    void log_persistsAuditLog() {
        // 准备：配置 Mock 返回值
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log("user-001", "CREATE", "PIPELINE", "pipe-1", Map.of("k", "v"));

        // 断言：校验响应或交互
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    /**
     * 测试方法 log_integratedWithControllers。
     */
    @Test
    @Disabled("无对外 API，待 AuditLogController 或 AOP 接入后补充集成场景")
    void log_integratedWithControllers() {
    }
}
