package com.dataflow.ai.business.engine.source;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.SourceConfig;

import java.util.List;

/**
 * 源读取器接口
 * 定义从不同类型数据源读取数据的统一接口
 */
public interface SourceReader {

    /**
     * 从数据源读取数据
     *
     * @param sourceConfig 源配置
     * @param context      执行上下文
     * @return 读取的记录列表
     * @throws Exception 读取过程中发生的异常
     */
    List<Record> read(SourceConfig sourceConfig, ExecutionContext context) throws Exception;

    /**
     * 获取支持的数据源类型
     *
     * @return 数据源类型
     */
    DataSourceType getSupportedType();

    /**
     * 测试数据源连接
     *
     * @param dataSource 数据源配置
     * @return 是否连接成功
     */
    boolean testConnection(DataSource dataSource);

    /**
     * 预览数据源数据（用于测试和验证）
     *
     * @param sourceConfig 源配置
     * @param sampleSize   采样大小
     * @return 预览数据
     * @throws Exception 预览过程中发生的异常
     */
    List<Record> preview(SourceConfig sourceConfig, int sampleSize) throws Exception;
}
