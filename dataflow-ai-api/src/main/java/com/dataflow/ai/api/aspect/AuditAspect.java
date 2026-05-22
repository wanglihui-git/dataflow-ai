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

/**
 * 审计日志 AOP 切面。
 * <p>
 * 在关键 Controller 方法成功返回后异步写入审计记录（登录、创建数据源/Pipeline、触发执行等）。
 * 未登录或单测无请求上下文时静默跳过。
 * </p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    /**
     * 登录成功后记录 LOGIN 审计。
     */
    @AfterReturning("execution(* com.dataflow.ai.api.controller.AuthController.login(..))")
    public void afterLogin() {
        write("LOGIN", "auth", null);
    }

    /**
     * 创建数据源成功后记录 CREATE 审计。
     */
    @AfterReturning("execution(* com.dataflow.ai.api.controller.DataSourceController.create(..))")
    public void afterDataSourceCreate() {
        write("CREATE", "data_source", null);
    }

    /**
     * 创建 Pipeline 成功后记录 CREATE 审计。
     */
    @AfterReturning("execution(* com.dataflow.ai.api.controller.PipelineController.create(..))")
    public void afterPipelineCreate() {
        write("CREATE", "pipeline", null);
    }

    /**
     * 触发 Pipeline 执行成功后记录 EXECUTE 审计。
     */
    @AfterReturning("execution(* com.dataflow.ai.api.controller.PipelineController.run(..))")
    public void afterPipelineRun() {
        write("EXECUTE", "pipeline", null);
    }

    /**
     * 写入一条审计日志。
     *
     * @param action       动作类型，如 LOGIN、CREATE、EXECUTE
     * @param resourceType 资源类型，如 auth、data_source、pipeline
     * @param resourceId   资源 ID（可为 null）
     */
    private void write(String action, String resourceType, String resourceId) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            HttpServletRequest request = currentRequest();
            auditLogService.log(userId, action, resourceType, resourceId, Map.of(),
                    request != null ? request.getRemoteAddr() : null,
                    request != null ? request.getHeader("User-Agent") : null);
        } catch (Exception ignored) {
            // 未登录或测试环境无 RequestContext 时跳过
        }
    }

    /**
     * 从 Spring 请求上下文获取当前 HttpServletRequest。
     *
     * @return 当前请求，无上下文时返回 null
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
