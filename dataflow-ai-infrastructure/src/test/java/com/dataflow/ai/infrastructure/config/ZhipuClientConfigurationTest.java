package com.dataflow.ai.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智谱 Bean 使用 app.llm.zhipu.*（非错误的 llm.zhipu.*）
 */
class ZhipuClientConfigurationTest {

    /**
     * 验证：zhipuLlmClient 使用 app.llm.zhipu 条件与配置。
     */
    @Test
    @DisplayName("zhipuLlmClient 使用 app.llm.zhipu 条件与配置")
    void zhipuLlmBeanUsesAppPrefix() throws Exception {
        Method method = AiClientConfiguration.class.getDeclaredMethod(
                "zhipuLlmClient",
                org.springframework.web.reactive.function.client.WebClient.Builder.class,
                String.class, String.class, String.class, int.class, double.class);
        ConditionalOnProperty onProperty = method.getAnnotation(ConditionalOnProperty.class);
        // 断言：校验响应或交互
        assertNotNull(onProperty);
        assertTrue(onProperty.name()[0].startsWith("app.llm"));
    }
}
