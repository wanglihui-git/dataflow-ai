package com.dataflow.ai.business.engine.transform;

import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.TransformContext;

/**
 * 转换处理器接口
 * 定义处理不同类型转换的统一接口
 */
public interface TransformProcessor {

    /**
     * 处理数据批次
     *
     * @param batch    输入数据批次
     * @param context  转换上下文
     * @return 处理后的数据批次
     * @throws Exception 处理过程中发生的异常
     */
    DataBatch process(DataBatch batch, TransformContext context) throws Exception;

    /**
     * 获取支持的转换类型
     *
     * @return 转换类型名称
     */
    String getSupportedType();

    /**
     * 验证转换配置
     *
     * @param context 转换上下文
     * @throws Exception 配置无效时抛出异常
     */
    default void validate(TransformContext context) throws Exception {
        // 默认实现：子类可覆写以校验节点 config
    }
}
