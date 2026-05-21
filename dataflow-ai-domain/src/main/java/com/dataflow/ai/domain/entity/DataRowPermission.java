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

    @Id
    private String id;

    private String dataSourceId;

    @Enumerated(EnumType.STRING)
    private UserRole targetRole;

    private String targetDepartment;

    private String targetUser;

    private String filterCondition;

    private Integer priority;

    private LocalDateTime createdAt;
}
