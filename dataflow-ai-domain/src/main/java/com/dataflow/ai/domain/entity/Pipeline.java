package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.domain.vo.ScheduleConfig;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.domain.vo.Transform;
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
public class Pipeline {

    /**
     * Pipeline ID
     */
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
    private SourceConfig source;

    /**
     * 转换节点列表
     */
    private List<Transform> transforms;

    /**
     * 目标配置
     */
    private SinkConfig sink;

    /**
     * 调度配置
     */
    private ScheduleConfig schedule;

    /**
     * 所有者ID
     */
    private String ownerId;

    /**
     * 权限级别
     */
    private PermissionLevel permissionLevel;

    /**
     * 允许的角色
     */
    private List<String> allowedRoles;

    /**
     * 允许的用户
     */
    private List<String> allowedUsers;

    /**
     * 允许的部门
     */
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
