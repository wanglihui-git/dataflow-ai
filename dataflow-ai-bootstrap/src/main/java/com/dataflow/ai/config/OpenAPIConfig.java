package com.dataflow.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DataFlow AI API")
                        .version("1.0.0")
                        .description("智能数据转换平台API文档")
                        .contact(new Contact()
                                .name("DataFlow Team")
                                .email("support@dataflow.ai")));
    }
}
