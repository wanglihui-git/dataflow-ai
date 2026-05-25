package com.dataflow.ai.domain.converter;

import com.dataflow.ai.domain.vo.Transform;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * List<Transform> ↔ JSON String 转换器（JSONB 列）
 */
@Converter
public class TransformListConverter extends AbstractJsonConverter<List<Transform>> {

    /** {@inheritDoc} */
    @Override
    protected TypeReference<List<Transform>> typeReference() {
        return new TypeReference<>() {};
    }
}
