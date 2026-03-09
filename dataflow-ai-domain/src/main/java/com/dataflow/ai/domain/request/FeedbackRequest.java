package com.dataflow.ai.domain.request;

import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 反馈请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    /**
     * AI辅助记录ID
     */
    private String aiHelperId;

    /**
     * 动作类型（accept-采纳, modify-修改后采纳, reject-拒绝）
     */
    private String action;

    /**
     * 修改后的节点（如果修改了）
     */
    private List<Transform> modifiedNodes;

    /**
     * 关联的Pipeline ID
     */
    private String pipelineId;
}
