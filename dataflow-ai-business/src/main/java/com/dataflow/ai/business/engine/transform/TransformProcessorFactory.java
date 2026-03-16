package com.dataflow.ai.business.engine.transform;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.impl.*;
import com.dataflow.ai.domain.enums.TransformType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换处理器工厂
 * 根据转换类型创建对应的处理器
 */
@Slf4j
@Component
public class TransformProcessorFactory {

    private final Map<TransformType, TransformProcessor> processors = new HashMap<>();

    @Resource
    private FieldMapperProcessor fieldMapperProcessor;

    @Resource
    private FilterProcessor filterProcessor;

    @Resource
    private FlattenProcessor flattenProcessor;

    @Resource
    private LookupProcessor lookupProcessor;

    @Resource
    private ScriptProcessor scriptProcessor;

    @Resource
    private AiAssistedProcessor aiAssistedProcessor;

    @Resource
    private AggregateProcessor aggregateProcessor;

    @Resource
    private JoinProcessor joinProcessor;

    @Resource
    private SortProcessor sortProcessor;

    @Resource
    private GroupProcessor groupProcessor;

    @PostConstruct
    public void init() {
        processors.put(TransformType.FIELD_MAPPER, fieldMapperProcessor);
        processors.put(TransformType.FILTER, filterProcessor);
        processors.put(TransformType.FLATTEN, flattenProcessor);
        processors.put(TransformType.LOOKUP, lookupProcessor);
        processors.put(TransformType.SCRIPT, scriptProcessor);
        processors.put(TransformType.AI_ASSISTED, aiAssistedProcessor);
        processors.put(TransformType.AGGREGATE, aggregateProcessor);
        processors.put(TransformType.JOIN, joinProcessor);
        processors.put(TransformType.SORT, sortProcessor);
        processors.put(TransformType.GROUP, groupProcessor);

        log.info("TransformProcessorFactory initialized with {} processor types", processors.size());
    }

    /**
     * 根据转换类型创建处理器
     */
    public TransformProcessor createProcessor(TransformType type) {
        if (type == null) {
            throw new TransformException("Transform type cannot be null");
        }

        TransformProcessor processor = processors.get(type);

        if (processor == null) {
            throw new TransformException("Unsupported transform type: " + type);
        }

        log.debug("Created transform processor for type: {}", type);
        return processor;
    }

    /**
     * 检查是否支持指定类型
     */
    public boolean isSupported(TransformType type) {
        return processors.containsKey(type);
    }

    /**
     * 获取所有支持的转换类型
     */
    public java.util.Set<TransformType> getSupportedTypes() {
        return processors.keySet();
    }
}
