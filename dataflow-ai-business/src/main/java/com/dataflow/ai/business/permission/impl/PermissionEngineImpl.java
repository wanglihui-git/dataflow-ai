package com.dataflow.ai.business.permission.impl;

import com.dataflow.ai.business.config.PermissionProperties;
import com.dataflow.ai.business.permission.PermissionEngine;
import com.dataflow.ai.business.permission.processor.MaskProcessor;
import com.dataflow.ai.business.repository.FieldPermissionRepository;
import com.dataflow.ai.business.repository.RowPermissionRepository;
import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.DataRowPermission;
import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.AccessType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionEngineImpl implements PermissionEngine {

    private final PermissionProperties permissionProperties;
    private final FieldPermissionRepository fieldPermissionRepository;
    private final RowPermissionRepository rowPermissionRepository;
    private final MaskProcessor maskProcessor;

    @Override
    public Map<String, Object> applyPermissions(Map<String, Object> row, String dataSourceId, User user) {
        if (!permissionProperties.isEnabled()) {
            return row;
        }
        Map<String, Object> copy = new HashMap<>(row);
        for (String field : new ArrayList<>(copy.keySet())) {
            List<DataFieldPermission> rules = fieldPermissionRepository.findMatchingRules(
                    dataSourceId, field, user.getId(), user.getRole().name(), user.getDepartment());
            DataFieldPermission rule = pickHighestPriority(rules);
            if (rule == null) {
                continue;
            }
            if (rule.getAccessType() == AccessType.NONE) {
                copy.remove(field);
            } else if (rule.getAccessType() == AccessType.MASKED) {
                copy.put(field, maskProcessor.mask(copy.get(field), rule.getMaskRule()));
            }
        }
        return copy;
    }

    @Override
    public List<Map<String, Object>> applyPermissions(List<Map<String, Object>> data,
                                                      String dataSourceId, User user) {
        if (!permissionProperties.isEnabled() || data == null) {
            return data;
        }
        List<DataRowPermission> rowRules = rowPermissionRepository.findByDataSourceId(dataSourceId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : data) {
            if (matchesRowFilters(row, rowRules, user)) {
                result.add(applyPermissions(row, dataSourceId, user));
            }
        }
        return result;
    }

    @Override
    public DataFieldPermission getFieldPermission(String dataSourceId, String fieldName, User user) {
        List<DataFieldPermission> rules = fieldPermissionRepository.findMatchingRules(
                dataSourceId, fieldName, user.getId(), user.getRole().name(), user.getDepartment());
        return pickHighestPriority(rules);
    }

    private DataFieldPermission pickHighestPriority(List<DataFieldPermission> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return rules.get(0);
    }

    private boolean matchesRowFilters(Map<String, Object> row, List<DataRowPermission> rules, User user) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        List<DataRowPermission> applicable = rules.stream()
                .filter(r -> matchesTarget(r, user))
                .toList();
        if (applicable.isEmpty()) {
            return true;
        }
        return applicable.stream()
                .anyMatch(r -> RowFilterEvaluator.matches(row, r.getFilterCondition()));
    }

    private boolean matchesTarget(DataRowPermission rule, User user) {
        if (rule.getTargetUser() != null && !rule.getTargetUser().isEmpty()) {
            return rule.getTargetUser().equals(user.getId());
        }
        if (rule.getTargetDepartment() != null && !rule.getTargetDepartment().isEmpty()) {
            return rule.getTargetDepartment().equals(user.getDepartment());
        }
        if (rule.getTargetRole() != null) {
            return rule.getTargetRole() == user.getRole();
        }
        return true;
    }
}
