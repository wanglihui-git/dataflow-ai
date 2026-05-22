package com.dataflow.ai.api.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模拟 JWT 过滤器写入的 principal（用户 ID 字符串），供 SecurityUtils 使用。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserIdSecurityContextFactory.class)
public @interface WithMockUserId {

    /** 模拟登录用户 ID，写入 Authentication principal */
    String value() default "user-001";

    /** 角色列表，工厂会自动补全 ROLE_ 前缀 */
    String[] roles() default {"DEVELOPER"};
}
