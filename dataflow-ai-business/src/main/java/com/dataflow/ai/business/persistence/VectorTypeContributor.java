package com.dataflow.ai.business.persistence;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;

/**
 * 向 Hibernate 注册 VectorType，使 float[] 字段自动映射到 PostgreSQL vector 类型
 * 通过 spring.jpa.properties.hibernate.metadata_builder_contributor 配置激活
 */
public class VectorTypeContributor implements MetadataBuilderContributor {

    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applyBasicType(new VectorType(), float[].class.getName());
    }
}
