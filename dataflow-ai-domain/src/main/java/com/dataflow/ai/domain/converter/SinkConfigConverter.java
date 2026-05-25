package com.dataflow.ai.domain.converter;

import com.dataflow.ai.domain.vo.SinkConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

/**
 * SinkConfig ↔ JSON String 转换器（JSONB 列）
 */
@Converter
public class SinkConfigConverter extends AbstractJsonConverter<SinkConfig> {

    /** {@inheritDoc} */
    @Override
    protected TypeReference<SinkConfig> typeReference() {
        return new TypeReference<>() {};
    }
}
