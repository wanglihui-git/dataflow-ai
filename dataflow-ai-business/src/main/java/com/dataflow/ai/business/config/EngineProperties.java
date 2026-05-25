package com.dataflow.ai.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据流执行引擎配置（前缀 {@code app.engine}）。
 * <p>控制 DAG 并行分组、Source/Sink 默认重试次数与退避间隔。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.engine")
public class EngineProperties {

    /**
     * 是否按 DAG 层级分组执行（同层多节点仍串行写回同一记录集，保证正确性）。
     */
    private boolean parallelDagEnabled = false;

    /**
     * Source/Sink 默认最大重试次数（可被 {@link com.dataflow.ai.domain.vo.ScheduleConfig#getRetryCount()} 覆盖）。
     */
    private int defaultMaxRetries = 3;

    /**
     * 重试初始间隔（毫秒），用于指数退避计算的基数。
     */
    private long retryIntervalMs = 1000;
}
