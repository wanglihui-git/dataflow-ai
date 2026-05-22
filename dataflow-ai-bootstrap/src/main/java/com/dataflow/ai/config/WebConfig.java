package com.dataflow.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 扩展配置：跨域（CORS）规则，便于前端开发环境访问 API。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 为 {@code /api/**} 路径启用 CORS，允许携带凭证与常见 HTTP 方法。
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
