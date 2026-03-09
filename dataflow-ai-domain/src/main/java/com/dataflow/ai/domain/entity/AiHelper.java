package com.dataflow.ai.domain.entity;

import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI辅助记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHelper {

    /**
     * 记录ID
     */
    private String id;

    /**
     * 用户指令
     */
    private String instruction;

    /**
     * 上下文信息
     */
    private Map<String, Object> context;

    /**
     * 生成的转换节点
     */
    private List<Transform> generatedNodes;

    /**
     * 关联的Pipeline ID
     */
    private String pipelineId;

    /**
     * 用户反馈（1-采纳, 0-拒绝, -1-修改后采纳）
     */
    private Integer userFeedback;

    /**
     * 指令的向量表示（用于相似度搜索）
     */
    private float[] embedding;

    /**
     * 创建者ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
