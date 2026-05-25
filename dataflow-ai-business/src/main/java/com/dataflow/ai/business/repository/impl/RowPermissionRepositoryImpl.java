package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.RowPermissionRepository;
import com.dataflow.ai.business.repository.jpa.RowPermissionJpaRepository;
import com.dataflow.ai.domain.entity.DataRowPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 行权限仓储实现（PostgreSQL）
 */
@Repository
@RequiredArgsConstructor
public class RowPermissionRepositoryImpl implements RowPermissionRepository {

    private final RowPermissionJpaRepository jpaRepository;

    /**
     * 根据数据源 ID 查询
     */
    @Override
    public List<DataRowPermission> findByDataSourceId(String dataSourceId) {
        return jpaRepository.findByDataSourceId(dataSourceId);
    }

    /**
     * 根据 ID 查询
     */
    @Override
    public Optional<DataRowPermission> findById(String id) {
        return jpaRepository.findById(id);
    }

    /**
     * 保存实体
     */
    @Override
    @Transactional
    public DataRowPermission save(DataRowPermission permission) {
        if (permission.getId() == null) {
            permission.setId(UUID.randomUUID().toString());
        }
        if (permission.getCreatedAt() == null) {
            permission.setCreatedAt(LocalDateTime.now());
        }
        return jpaRepository.save(permission);
    }

    /**
     * 根据 ID 删除
     */
    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
