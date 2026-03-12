package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.FieldPermissionRepository;
import com.dataflow.ai.business.repository.jpa.FieldPermissionJpaRepository;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 字段权限Repository实现（PostgreSQL）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FieldPermissionRepositoryImpl implements FieldPermissionRepository {

    private final FieldPermissionJpaRepository jpaRepository;

    @Override
    public List<DataFieldPermission> findByDataSourceId(String dataSourceId) {
        return jpaRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    public List<DataFieldPermission> findMatchingRules(String dataSourceId, String fieldName,
                                                       String userId, String role, String department) {
        return jpaRepository.findByDataSourceId(dataSourceId).stream()
                .filter(p -> p.getColumnName() == null || p.getColumnName().equals(fieldName))
                .filter(p -> matchesUser(p, userId, role, department))
                .collect(Collectors.toList());
    }

    private boolean matchesUser(DataFieldPermission permission, String userId, String role, String department) {
        if (permission.getTargetUser() != null && !permission.getTargetUser().isEmpty()) {
            return permission.getTargetUser().equals(userId);
        }
        if (permission.getTargetDepartment() != null && !permission.getTargetDepartment().isEmpty()) {
            return permission.getTargetDepartment().equals(department);
        }
        if (permission.getTargetRole() != null) {
            return permission.getTargetRole().name().equals(role);
        }
        return false;
    }

    @Override
    @Transactional
    public DataFieldPermission save(DataFieldPermission permission) {
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

    @Override
    public Page<DataFieldPermission> findByDataSourceId(String dataSourceId, Pageable pageable) {
        return jpaRepository.findByDataSourceId(dataSourceId, pageable);
    }
}
