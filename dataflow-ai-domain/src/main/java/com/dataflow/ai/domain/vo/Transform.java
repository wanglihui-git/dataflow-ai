package com.dataflow.ai.domain.vo;

import com.dataflow.ai.domain.enums.TransformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 转换节点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transform {

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 转换类型
     */
    private TransformType type;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 配置（不同type有不同结构）
     */
    private Map<String, Object> config;

    /**
     * 依赖节点ID列表
     */
    private List<String> dependsOn;

    /**
     * 生成者（如果是AI生成的，记录AI辅助节点ID）
     */
    private String generatedBy;
}
