package com.dataflow.ai.api.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.List;

/**
 * {@link WithMockUserId} 注解对应的 SecurityContext 工厂。
 * <p>将 principal 设为用户 ID 字符串，并附加 ROLE_* 权限，供 {@code SecurityUtils} 读取。</p>
 */
public class WithMockUserIdSecurityContextFactory implements WithSecurityContextFactory<WithMockUserId> {

    /**
     * 根据注解参数构建认证上下文。
     *
     * @param annotation 测试方法或类上的 {@link WithMockUserId}
     * @return 已填充 Authentication 的 SecurityContext
     */
    @Override
    public SecurityContext createSecurityContext(WithMockUserId annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        // 角色名统一加 ROLE_ 前缀以符合 Spring Security 约定
        List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(annotation.value(), null, authorities);
        context.setAuthentication(authentication);
        return context;
    }
}
