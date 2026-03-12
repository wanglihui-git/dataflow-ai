package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.enums.AccessType;
import com.dataflow.ai.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据字段权限实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "data_field_permissions")
public class DataFieldPermission {

    /**
     * 权限ID
     */
    @Id
    private String id;

    /**
     * 数据源ID
     */
    private String dataSourceId;

    /**
     * 字段名称
     */
    private String columnName;

    /**
     * 目标角色
     */
    @Enumerated(EnumType.STRING)
    private UserRole targetRole;

    /**
     * 目标部门
     */
    private String targetDepartment;

    /**
     * 目标用户
     */
    private String targetUser;

    /**
     * 访问类型
     */
    @Enumerated(EnumType.STRING)
    private AccessType accessType;

    /**
     * 脱敏规则
     */
    private String maskRule;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 权限类型
     */
    public enum PermissionType {
        /**
         * 行级权限
         */
        ROW_PERMISSION,

        /**
         * 列级权限
         */
        COLUMN_PERMISSION
    }
}
