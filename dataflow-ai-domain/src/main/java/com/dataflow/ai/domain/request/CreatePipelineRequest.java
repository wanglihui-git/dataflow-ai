package com.dataflow.ai.domain.request;

import com.dataflow.ai.domain.vo.ScheduleConfig;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建Pipeline请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePipelineRequest {

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
     * 权限级别
     */
    private String permissionLevel;

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
}
