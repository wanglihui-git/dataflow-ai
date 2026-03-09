package com.dataflow.ai.business.repository;

import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;

import java.util.List;
import java.util.Optional;

/**
 * 数据源Repository接口
 */
public interface DataSourceRepository {

    /**
     * 根据ID查询数据源
     */
    Optional<DataSource> findById(String id);

    /**
     * 根据创建者查询数据源列表
     */
    List<DataSource> findByCreatedBy(String createdBy);

    /**
     * 根据类型查询数据源列表
     */
    List<DataSource> findByType(DataSourceType type);

    /**
     * 查询所有数据源
     */
    List<DataSource> findAll();

    /**
     * 保存数据源
     */
    DataSource save(DataSource dataSource);

    /**
     * 删除数据源
     */
    void deleteById(String id);

    /**
     * 根据名称查询数据源
     */
    Optional<DataSource> findByName(String name);
}
