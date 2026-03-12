package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.converter.ScheduleConfigConverter;
import com.dataflow.ai.domain.converter.SinkConfigConverter;
import com.dataflow.ai.domain.converter.SourceConfigConverter;
import com.dataflow.ai.domain.converter.StringListConverter;
import com.dataflow.ai.domain.converter.TransformListConverter;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.domain.vo.ScheduleConfig;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.domain.vo.Transform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pipeline实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pipelines")
public class Pipeline {

    /**
     * Pipeline ID
     */
    @Id
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 源配置
     */
    @Convert(converter = SourceConfigConverter.class)
    @Column(columnDefinition = "jsonb")
    private SourceConfig source;

    /**
     * 转换节点列表
     */
    @Convert(converter = TransformListConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<Transform> transforms;

    /**
     * 目标配置
     */
    @Convert(converter = SinkConfigConverter.class)
    @Column(columnDefinition = "jsonb")
    private SinkConfig sink;

    /**
     * 调度配置
     */
    @Convert(converter = ScheduleConfigConverter.class)
    @Column(columnDefinition = "jsonb")
    private ScheduleConfig schedule;

    /**
     * 所有者ID
     */
    private String ownerId;

    /**
     * 权限级别
     */
    @Enumerated(EnumType.STRING)
    private PermissionLevel permissionLevel;

    /**
     * 允许的角色
     */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<String> allowedRoles;

    /**
     * 允许的用户
     */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<String> allowedUsers;

    /**
     * 允许的部门
     */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "jsonb")
    private List<String> allowedDepartments;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 权限级别枚举
     */
    public enum PermissionLevel {
        /**
         * 私有
         */
        PRIVATE,

        /**
         * 共享
         */
        SHARED,

        /**
         * 公开
         */
        PUBLIC
    }
}
