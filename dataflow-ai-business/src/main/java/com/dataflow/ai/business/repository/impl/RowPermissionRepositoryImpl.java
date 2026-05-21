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

@Repository
@RequiredArgsConstructor
public class RowPermissionRepositoryImpl implements RowPermissionRepository {

    private final RowPermissionJpaRepository jpaRepository;

    @Override
    public List<DataRowPermission> findByDataSourceId(String dataSourceId) {
        return jpaRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    public Optional<DataRowPermission> findById(String id) {
        return jpaRepository.findById(id);
    }

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

    @Override
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
