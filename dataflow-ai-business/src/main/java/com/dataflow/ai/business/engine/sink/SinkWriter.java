package com.dataflow.ai.business.engine.sink;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.vo.SinkConfig;

/**
 * 目标写入器接口
 * 定义向不同类型目标写入数据的统一接口
 */
public interface SinkWriter {

    /**
     * 写入数据批次到目标
     *
     * @param batch         数据批次
     * @param sinkConfig    目标配置
     * @param context       执行上下文
     * @return 写入的记录数
     * @throws Exception 写入过程中发生的异常
     */
    long write(DataBatch batch, SinkConfig sinkConfig, ExecutionContext context) throws Exception;

    /**
     * 获取支持的数据源类型
     *
     * @return 数据源类型
     */
    String getSupportedType();

    /**
     * 测试目标连接
     *
     * @param dataSource 数据源配置
     * @return 是否连接成功
     */
    boolean testConnection(DataSource dataSource);

    /**
     * 初始化目标（如创建表、准备文件等）
     *
     * @param sinkConfig 目标配置
     * @param context    执行上下文
     * @throws Exception 初始化过程中发生的异常
     */
    default void initialize(SinkConfig sinkConfig, ExecutionContext context) throws Exception {
        // 默认不执行初始化
    }

    /**
     * 完成写入后的清理工作
     *
     * @param sinkConfig 目标配置
     * @param context    执行上下文
     * @throws Exception 清理过程中发生的异常
     */
    default void cleanup(SinkConfig sinkConfig, ExecutionContext context) throws Exception {
        // 默认不执行清理
    }
}
