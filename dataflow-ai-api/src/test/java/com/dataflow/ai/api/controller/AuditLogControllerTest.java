package com.dataflow.ai.api.controller;

import com.dataflow.ai.api.support.TestSecurityConfig;
import com.dataflow.ai.business.service.AuditLogService;
import com.dataflow.ai.domain.entity.AuditLog;
import com.dataflow.ai.domain.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuditLogController 管理员审计分页与 403 测试。
 */

@WebMvcTest
@Import({AuditLogController.class, TestSecurityConfig.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    /**
     * 验证：GET /v1/audit-logs - 管理员分页查询。
     */
    @Test
    @DisplayName("GET /v1/audit-logs - 管理员分页查询")
    @WithMockUser(roles = "ADMIN")
    void list_withAdmin_success() throws Exception {
        AuditLog log = AuditLog.builder()
                .id(1001L)
                .userId("user-001")
                .action("LOGIN")
                .resourceType("USER")
                .resourceId("user-001")
                .details(Map.of("ip", "127.0.0.1"))
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.of(2026, 5, 22, 9, 0))
                .build();
        PageResponse<AuditLog> page = PageResponse.of(List.of(log), 0, 20, 1);
        // 准备：配置 Mock 返回值
        when(auditLogService.findPage(eq("user-001"), eq("LOGIN"), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);

        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/audit-logs")
                        .param("userId", "user-001")
                        .param("action", "LOGIN")
                        .param("page", "0")
                        .param("size", "20"))
                // 断言：校验响应或交互
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].action").value("LOGIN"))
                // 断言：校验响应或交互
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(auditLogService).findPage(eq("user-001"), eq("LOGIN"), isNull(), isNull(), any(PageRequest.class));
    }

    /**
     * 验证：GET /v1/audit-logs - 非管理员 403。
     */
    @Test
    @DisplayName("GET /v1/audit-logs - 非管理员 403")
    @WithMockUser(roles = "DEVELOPER")
    void list_withoutAdmin_forbidden() throws Exception {
        // 执行：发起 HTTP 请求
        mockMvc.perform(get("/v1/audit-logs"))
                // 断言：校验响应或交互
                .andExpect(status().isForbidden());
    }
}
