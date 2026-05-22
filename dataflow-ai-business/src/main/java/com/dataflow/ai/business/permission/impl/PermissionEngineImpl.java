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

/**
 * {@link PermissionEngine} 实现。
 * <p>按数据源加载列/行规则，对单行脱敏或批量过滤后脱敏；可通过配置全局关闭。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionEngineImpl implements PermissionEngine {

    private final PermissionProperties permissionProperties;
    private final FieldPermissionRepository fieldPermissionRepository;
    private final RowPermissionRepository rowPermissionRepository;
    private final MaskProcessor maskProcessor;

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public DataFieldPermission getFieldPermission(String dataSourceId, String fieldName, User user) {
        List<DataFieldPermission> rules = fieldPermissionRepository.findMatchingRules(
                dataSourceId, fieldName, user.getId(), user.getRole().name(), user.getDepartment());
        return pickHighestPriority(rules);
    }

    /**
     * 取匹配规则列表中优先级最高的一条（仓储层已排序时取首条）。
     *
     * @param rules 候选规则
     * @return 生效规则，无匹配则为 null
     */
    private DataFieldPermission pickHighestPriority(List<DataFieldPermission> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return rules.get(0);
    }

    /**
     * 判断数据行是否通过行级过滤（无规则或当前用户无专属规则时默认放行）。
     *
     * @param row   数据行
     * @param rules 数据源下全部行权限规则
     * @param user  当前用户
     * @return 应保留并继续列脱敏时返回 true
     */
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
        // 任一适用规则的条件匹配即保留该行
        return applicable.stream()
                .anyMatch(r -> RowFilterEvaluator.matches(row, r.getFilterCondition()));
    }

    /**
     * 判断行权限规则是否作用于当前用户（按用户 ID、部门或角色匹配）。
     */
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
