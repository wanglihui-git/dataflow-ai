package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.domain.entity.Pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline 访问权限合并与过滤工具
 */
final class PipelineAccessHelper {

    private PipelineAccessHelper() {
    }

    /**
     * 合并用户拥有的、公开的及共享 Pipeline，按 ID 去重。
     */
    static List<Pipeline> mergeAccessible(List<Pipeline> owned, List<Pipeline> publicPipelines,
                                          List<Pipeline> shared, String userId, String role, String department) {
        Map<String, Pipeline> map = new LinkedHashMap<>();
        owned.forEach(p -> map.put(p.getId(), p));
        publicPipelines.forEach(p -> map.put(p.getId(), p));
        // 共享 Pipeline 需匹配用户/角色/部门白名单
        shared.stream()
                .filter(p -> matchesShared(p, userId, role, department))
                .forEach(p -> map.put(p.getId(), p));
        return new ArrayList<>(map.values());
    }

    /**
     * 判断共享 Pipeline 是否对当前用户可见。
     */
    static boolean matchesShared(Pipeline pipeline, String userId, String role, String department) {
        if (pipeline.getAllowedUsers() != null && pipeline.getAllowedUsers().contains(userId)) {
            return true;
        }
        if (pipeline.getAllowedRoles() != null && role != null && pipeline.getAllowedRoles().contains(role)) {
            return true;
        }
        return pipeline.getAllowedDepartments() != null && department != null
                && pipeline.getAllowedDepartments().contains(department);
    }

    /**
     * 按名称子串过滤（忽略大小写）；名称为空时返回原列表。
     */
    static List<Pipeline> filterByName(List<Pipeline> pipelines, String name) {
        if (name == null || name.isBlank()) {
            return pipelines;
        }
        String lower = name.toLowerCase();
        return pipelines.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(lower))
                .toList();
    }
}
