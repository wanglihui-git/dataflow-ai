package com.dataflow.ai.api;

import org.springframework.boot.SpringBootConfiguration;

/**
 * API 模块切片测试用启动配置（仅作 {@code @WebMvcTest} 锚点，不启用完整自动配置）。
 * 主应用 {@code DataFlowApplication} 位于 dataflow-ai-bootstrap。
 */
@SpringBootConfiguration
public class ApiTestApplication {
}
