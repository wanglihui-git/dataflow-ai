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

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("log - 写入审计记录")
    void log_persistsAuditLog() {
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log("user-001", "CREATE", "PIPELINE", "pipe-1", Map.of("k", "v"));

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @Disabled("无对外 API，待 AuditLogController 或 AOP 接入后补充集成场景")
    void log_integratedWithControllers() {
    }
}
