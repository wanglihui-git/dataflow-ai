package com.dataflow.ai.api.aspect;

import com.dataflow.ai.business.service.AuditLogService;
import com.dataflow.ai.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning("execution(* com.dataflow.ai.api.controller.AuthController.login(..))")
    public void afterLogin() {
        write("LOGIN", "auth", null);
    }

    @AfterReturning("execution(* com.dataflow.ai.api.controller.DataSourceController.create(..))")
    public void afterDataSourceCreate() {
        write("CREATE", "data_source", null);
    }

    @AfterReturning("execution(* com.dataflow.ai.api.controller.PipelineController.create(..))")
    public void afterPipelineCreate() {
        write("CREATE", "pipeline", null);
    }

    @AfterReturning("execution(* com.dataflow.ai.api.controller.PipelineController.run(..))")
    public void afterPipelineRun() {
        write("EXECUTE", "pipeline", null);
    }

    private void write(String action, String resourceType, String resourceId) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            HttpServletRequest request = currentRequest();
            auditLogService.log(userId, action, resourceType, resourceId, Map.of(),
                    request != null ? request.getRemoteAddr() : null,
                    request != null ? request.getHeader("User-Agent") : null);
        } catch (Exception ignored) {
            // 未登录或测试环境无请求上下文时跳过
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
