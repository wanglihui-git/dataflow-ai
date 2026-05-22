package com.dataflow.ai.domain.converter;

import com.dataflow.ai.domain.vo.ScheduleConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;

/**
 * ScheduleConfig ↔ JSON String 转换器（JSONB 列）
 */
@Converter
public class ScheduleConfigConverter extends AbstractJsonConverter<ScheduleConfig> {

    /** {@inheritDoc} */
    @Override
    protected TypeReference<ScheduleConfig> typeReference() {
        return new TypeReference<>() {};
    }
}
