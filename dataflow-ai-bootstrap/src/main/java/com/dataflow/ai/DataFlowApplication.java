package com.dataflow.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * DataFlow AI 应用启动类
 */
@SpringBootApplication
@EnableAsync
public class DataFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataFlowApplication.class, args);
    }
}
