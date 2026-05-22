package com.dataflow.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Knife4j 文档元信息配置（标题、版本、联系方式）。
 */
@Configuration
public class OpenAPIConfig {

    /**
     * 注册全局 OpenAPI 文档描述，供 Swagger UI / doc.html 展示。
     *
     * @return OpenAPI 根对象
     */
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
