package com.dataflow.ai.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类：获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 ID
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        return (String) authentication.getPrincipal();
    }

    /**
     * 获取当前登录用户的角色
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