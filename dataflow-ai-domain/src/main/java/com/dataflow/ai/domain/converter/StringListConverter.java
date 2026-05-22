package com.dataflow.ai.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * List<String> ↔ JSON String 转换器（JSONB 列）
 */
@Converter
public class StringListConverter extends AbstractJsonConverter<List<String>> {

    /** {@inheritDoc} */
    @Override
    protected TypeReference<List<String>> typeReference() {
        return new TypeReference<>() {};
    }
}
