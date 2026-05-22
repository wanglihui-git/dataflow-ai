package com.dataflow.ai.api.interceptor;

import com.dataflow.ai.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文 MVC 拦截器。
 * <p>
 * 在请求进入 Controller 前将 userId、requestId 写入 SLF4J {@link MDC}，
 * 便于日志聚合与链路追踪；请求结束后清理 MDC 避免线程池污染。
 * </p>
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    /** MDC 中当前用户 ID 的键名 */
    public static final String MDC_USER_ID = "userId";
    /** MDC 中请求追踪 ID 的键名 */
    public static final String MDC_REQUEST_ID = "requestId";

    /**
     * 请求前：生成或透传 requestId，并尝试写入当前用户 ID。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  目标处理器
     * @return 始终 true 以继续处理链
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(MDC_REQUEST_ID, requestId);
        try {
            String userId = SecurityUtils.getCurrentUserId();
            MDC.put(MDC_USER_ID, userId);
        } catch (Exception ignored) {
            // 未认证路径（如 /v1/auth/login）无 principal，跳过 userId
        }
        return true;
    }

    /**
     * 请求完成后清理 MDC，避免线程复用时残留上下文。
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  目标处理器
     * @param ex       处理过程中抛出的异常（可为 null）
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_REQUEST_ID);
    }
}
