package com.dataflow.ai.business.repository.impl;

import com.dataflow.ai.domain.entity.Pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PipelineAccessHelper {

    private PipelineAccessHelper() {
    }

    static List<Pipeline> mergeAccessible(List<Pipeline> owned, List<Pipeline> publicPipelines,
                                          List<Pipeline> shared, String userId, String role, String department) {
        Map<String, Pipeline> map = new LinkedHashMap<>();
        owned.forEach(p -> map.put(p.getId(), p));
        publicPipelines.forEach(p -> map.put(p.getId(), p));
        shared.stream()
                .filter(p -> matchesShared(p, userId, role, department))
                .forEach(p -> map.put(p.getId(), p));
        return new ArrayList<>(map.values());
    }

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
