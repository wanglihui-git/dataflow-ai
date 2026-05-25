package com.dataflow.ai.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * Map<String, Object> ↔ JSON String 转换器（JSONB 列）
 */
@Converter(autoApply = true)
public class JsonMapConverter extends AbstractJsonConverter<Map<String, Object>> {

    /** {@inheritDoc} */
    @Override
    protected TypeReference<Map<String, Object>> typeReference() {
        return new TypeReference<>() {};
    }
}
