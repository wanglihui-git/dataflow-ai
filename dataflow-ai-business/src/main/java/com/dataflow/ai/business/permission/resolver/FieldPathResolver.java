package com.dataflow.ai.business.permission.resolver;

import java.util.Collections;
import java.util.Set;

/**
 * 字段路径解析器 - SPI接口
 * 用于从复杂数据结构中提取字段值
 * 当前实现：只支持顶层字段
 * 未来扩展：支持嵌套路径、数组等
 */
public interface FieldPathResolver {

    /**
     * 判断配置的字段规则是否匹配实际字段
     * @param rulePattern 配置的规则（如 "phone" 或 "user.address.city"）
     * @param actualField 实际字段路径
     * @return 是否匹配
     */
    boolean matches(String rulePattern, String actualField);

    /**
     * 从数据中提取指定路径的值
     * @param data 原始数据（可能是Map、List或普通对象）
     * @param fieldPath 字段路径
     * @return 提取的值
     */
    Object extractValue(Object data, String fieldPath);

    /**
     * 获取字段路径的所有可能变体
     * 用于权限匹配时的快速查找
     */
    default Set<String> getPossiblePaths(Object data) {
        return Collections.emptySet();
    }
}