package com.dataflow.ai.business.permission;

import com.dataflow.ai.domain.entity.DataFieldPermission;
import com.dataflow.ai.domain.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 权限引擎 - 核心接口
 * 负责字段级权限的解析和脱敏
 */
public interface PermissionEngine {

    /**
     * 对单行数据应用字段权限
     * @param row 原始数据行
     * @param dataSourceId 数据源ID
     * @param user 当前用户
     * @return 脱敏后的数据行
     */
    Map<String, Object> applyPermissions(Map<String, Object> row,
                                         String dataSourceId,
                                         User user);

    /**
     * 对多行数据批量应用字段权限
     * @param data 原始数据列表
     * @param dataSourceId 数据源ID
     * @param user 当前用户
     * @return 脱敏后的数据列表
     */
    List<Map<String, Object>> applyPermissions(List<Map<String, Object>> data,
                                               String dataSourceId,
                                               User user);

    /**
     * 获取指定字段的权限信息（不脱敏，只返回规则）
     * 用于前端展示字段的权限状态
     */
    DataFieldPermission getFieldPermission(String dataSourceId,
                                           String fieldName,
                                           User user);
}