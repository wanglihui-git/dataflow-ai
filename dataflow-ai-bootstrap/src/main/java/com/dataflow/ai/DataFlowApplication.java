package com.dataflow.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DataFlow AI 平台 Spring Boot 主启动类。
 * <p>扫描 {@code com.dataflow.ai} 包下组件；启用异步任务与定时调度（Pipeline 调度等）。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DataFlowApplication {

    /**
     * 应用入口：启动内嵌 Tomcat 并加载全部自动配置。
     *
     * @param args 命令行参数（如 {@code --spring.profiles.active=dev}）
     */
    public static void main(String[] args) {
        SpringApplication.run(DataFlowApplication.class, args);
    }
}
