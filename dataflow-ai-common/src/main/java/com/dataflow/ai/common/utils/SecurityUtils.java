package com.dataflow.ai.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类：从 Spring Security 中读取当前登录用户 ID 与角色。
 * <p>Principal 由 {@link com.dataflow.ai.infrastructure.security.JwtAuthenticationFilter} 设置为 userId 字符串。
 */
public class SecurityUtils {

    /**
     * 获取当前已认证用户的 ID（JWT 过滤器写入的 Principal）。
     *
     * @return 用户 ID
     * @throws IllegalStateException 未登录或认证信息无效时
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        return (String) authentication.getPrincipal();
    }

    /**
     * 获取当前用户的首个 GrantedAuthority（通常为 {@code ROLE_*} 形式）。
     *
     * @return 权限字符串，未登录时返回 {@code null}
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse(null);
    }
}
