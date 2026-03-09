package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据源Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class DataSourceRepositoryImpl implements DataSourceRepository {

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    @Override
    public Optional<DataSource> findById(String id) {
        return Optional.ofNullable(dataSources.get(id));
    }

    @Override
    public List<DataSource> findByCreatedBy(String createdBy) {
        return dataSources.values().stream()
                .filter(ds -> ds.getCreatedBy().equals(createdBy))
                .collect(Collectors.toList());
    }

    @Override
    public List<DataSource> findByType(DataSourceType type) {
        return dataSources.values().stream()
                .filter(ds -> ds.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<DataSource> findAll() {
        return new ArrayList<>(dataSources.values());
    }

    @Override
    public DataSource save(DataSource dataSource) {
        if (dataSource.getId() == null) {
            dataSource.setId(UUID.randomUUID().toString());
        }
        if (dataSource.getCreatedAt() == null) {
            dataSource.setCreatedAt(LocalDateTime.now());
        }
        dataSource.setUpdatedAt(LocalDateTime.now());
        dataSources.put(dataSource.getId(), dataSource);
        return dataSource;
    }

    @Override
    public void deleteById(String id) {
        dataSources.remove(id);
    }

    @Override
    public Optional<DataSource> findByName(String name) {
        return dataSources.values().stream()
                .filter(ds -> ds.getName().equals(name))
                .findFirst();
    }
}
