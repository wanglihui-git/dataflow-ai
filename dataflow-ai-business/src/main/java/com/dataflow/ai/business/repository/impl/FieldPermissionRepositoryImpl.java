package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.business.repository.FieldPermissionRepository;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字段权限Repository实现（内存存储版本）
 */
@Slf4j
@Repository
public class FieldPermissionRepositoryImpl implements FieldPermissionRepository {

    private final Map<String, DataFieldPermission> permissions = new ConcurrentHashMap<>();

    @Override
    public List<DataFieldPermission> findByDataSourceId(String dataSourceId) {
        return permissions.values().stream()
                .filter(p -> p.getDataSourceId().equals(dataSourceId))
                .collect(Collectors.toList());
    }

    @Override
    public List<DataFieldPermission> findMatchingRules(String dataSourceId, String fieldName, String userId, String role, String department) {
        return permissions.values().stream()
                .filter(p -> p.getDataSourceId().equals(dataSourceId))
                .filter(p -> p.getColumnName().equals(fieldName) || p.getColumnName() == null)
                .filter(p -> matchesUser(p, userId, role, department))
                .collect(Collectors.toList());
    }

    private boolean matchesUser(DataFieldPermission permission, String userId, String role, String department) {
        // 检查用户ID
        if (permission.getTargetUser() != null && !permission.getTargetUser().isEmpty()) {
            return permission.getTargetUser().equals(userId);
        }
        // 检查部门
        if (permission.getTargetDepartment() != null && !permission.getTargetDepartment().isEmpty()) {
            return permission.getTargetDepartment().equals(department);
        }
        // 检查角色
        if (permission.getTargetRole() != null) {
            return permission.getTargetRole().name().equals(role);
        }
        return false;
    }

    @Override
    public DataFieldPermission save(DataFieldPermission permission) {
        if (permission.getId() == null) {
            permission.setId(UUID.randomUUID().toString());
        }
        if (permission.getCreatedAt() == null) {
            permission.setCreatedAt(LocalDateTime.now());
        }
        permissions.put(permission.getId(), permission);
        return permission;
    }

    @Override
    public void deleteById(String id) {
        permissions.remove(id);
    }

    @Override
    public Page<DataFieldPermission> findByDataSourceId(String dataSourceId, Pageable pageable) {
        List<DataFieldPermission> all = findByDataSourceId(dataSourceId);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
