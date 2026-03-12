package com.dataflow.ai.domain.converter;

import com.dataflow.ai.domain.vo.SourceConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

/**
 * SourceConfig ↔ JSON String 转换器（JSONB 列）
 */
@Converter
public class SourceConfigConverter extends AbstractJsonConverter<SourceConfig> {

    @Override
    protected TypeReference<SourceConfig> typeReference() {
        return new TypeReference<>() {};
    }
}
