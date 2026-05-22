package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据行级权限
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "data_row_permissions")
public class DataRowPermission {

    /** 规则 ID */
    @Id
    private String id;

    /** 数据源 ID */
    private String dataSourceId;

    /** 目标角色 */
    @Enumerated(EnumType.STRING)
    private UserRole targetRole;

    /** 目标部门 */
    private String targetDepartment;

    /** 目标用户 */
    private String targetUser;

    /** 行过滤条件表达式 */
    private String filterCondition;

    /** 优先级，数值越小越优先 */
    private Integer priority;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
