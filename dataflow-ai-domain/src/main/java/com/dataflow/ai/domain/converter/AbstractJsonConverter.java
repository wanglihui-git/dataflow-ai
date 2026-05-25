package com.dataflow.ai.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;

/**
 * JSON AttributeConverter 抽象基类
 */
public abstract class AbstractJsonConverter<T> implements AttributeConverter<T, String> {

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 子类提供 Jackson 反序列化用的类型引用。
     *
     * @return 类型引用
     */
    protected abstract TypeReference<T> typeReference();

    /**
     * 将实体属性序列化为 JSON 字符串写入数据库列。
     *
     * @param attribute 实体属性值
     * @return JSON 字符串，null 时返回 null
     */
    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize to JSON: " + attribute, e);
        }
    }

    /**
     * 将数据库 JSON 字符串反序列化为实体属性。
     *
     * @param dbData 数据库列值
     * @return 实体属性，空串时返回 null
     */
    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, typeReference());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize from JSON: " + dbData, e);
        }
    }
}
