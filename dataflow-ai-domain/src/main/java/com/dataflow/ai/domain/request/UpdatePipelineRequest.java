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
 * 更新 Pipeline 请求体（字段均为可选，仅非 null 字段会写入数据库）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePipelineRequest {

    private String name;

    private String description;

    private SourceConfig source;

    private List<Transform> transforms;

    private SinkConfig sink;

    private ScheduleConfig schedule;

    private String permissionLevel;

    private List<String> allowedRoles;

    private List<String> allowedUsers;

    private List<String> allowedDepartments;

    private String status;
}
