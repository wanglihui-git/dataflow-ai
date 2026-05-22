package com.dataflow.ai.api.config;

import com.dataflow.ai.api.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 扩展配置。
 * <p>
 * 注册 {@link UserContextInterceptor}，为 {@code /v1/**} 业务 API 注入 MDC 上下文。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserContextInterceptor userContextInterceptor;

    /**
     * 注册拦截器：覆盖所有 v1 版本 REST 路径。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor).addPathPatterns("/v1/**");
    }
}
