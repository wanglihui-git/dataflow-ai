package com.dataflow.ai.business.permission.impl;

import java.util.Map;

/**
 * 简单行过滤：filter_condition 格式 field=value 或 field!=value
 */
public final class RowFilterEvaluator {

    private RowFilterEvaluator() {
    }

    public static boolean matches(Map<String, Object> row, String filterCondition) {
        if (filterCondition == null || filterCondition.isBlank()) {
            return true;
        }
        String trimmed = filterCondition.trim();
        if (trimmed.contains("!=")) {
            String[] parts = trimmed.split("!=", 2);
            return !valueEquals(row.get(parts[0].trim()), parts[1].trim());
        }
        if (trimmed.contains("=")) {
            String[] parts = trimmed.split("=", 2);
            return valueEquals(row.get(parts[0].trim()), parts[1].trim());
        }
        return true;
    }

    private static boolean valueEquals(Object actual, String expected) {
        return actual != null && String.valueOf(actual).equals(expected);
    }
}
