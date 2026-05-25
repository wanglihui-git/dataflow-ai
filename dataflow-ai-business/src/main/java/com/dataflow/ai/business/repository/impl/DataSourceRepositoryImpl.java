package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.business.repository.jpa.DataSourceJpaRepository;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 数据源Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DataSourceRepositoryImpl implements DataSourceRepository {

    private final DataSourceJpaRepository jpaRepository;

    /**
     * 根据 ID 查询
     */
    @Override
    public Optional<DataSource> findById(String id) {
        return jpaRepository.findById(id);
    }

    /**
     * 根据创建人查询
     */
    @Override
    public List<DataSource> findByCreatedBy(String createdBy) {
        return jpaRepository.findByCreatedBy(createdBy);
    }

    /**
     * 根据类型查询
     */
    @Override
    public List<DataSource> findByType(DataSourceType type) {
        return jpaRepository.findByType(type);
    }

    /**
     * 查询全部
     */
    @Override
    public List<DataSource> findAll() {
        return jpaRepository.findAll();
    }

    /**
     * 保存实体
     */
    @Override
    @Transactional
    public DataSource save(DataSource dataSource) {
        if (dataSource.getId() == null) {
            dataSource.setId(UUID.randomUUID().toString());
        }
        if (dataSource.getCreatedAt() == null) {
            dataSource.setCreatedAt(LocalDateTime.now());
        }
        dataSource.setUpdatedAt(LocalDateTime.now());
        return jpaRepository.save(dataSource);
    }

    /**
     * 根据 ID 删除
     */
    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    /**
     * 根据名称查询
     */
    @Override
    public Optional<DataSource> findByName(String name) {
        return jpaRepository.findByName(name);
    }
}
