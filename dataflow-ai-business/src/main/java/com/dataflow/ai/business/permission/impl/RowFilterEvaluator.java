package com.dataflow.ai.business.permission.impl;

import java.util.Map;

/**
 * 行级过滤条件求值器。
 * <p>支持简单表达式：{@code field=value} 或 {@code field!=value}，空条件视为恒真。</p>
 */
public final class RowFilterEvaluator {

    private RowFilterEvaluator() {
    }

    /**
     * 判断数据行是否满足过滤条件。
     *
     * @param row             数据行 Map
     * @param filterCondition 过滤表达式，null 或空白视为匹配
     * @return 满足条件返回 true
     */
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

    /**
     * 比较字段值与期望字符串是否相等。
     */
    private static boolean valueEquals(Object actual, String expected) {
        return actual != null && String.valueOf(actual).equals(expected);
    }
}
