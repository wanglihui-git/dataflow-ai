package com.dataflow.ai.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponse {

    /**
     * 执行记录ID
     */
    private String runId;

    /**
     * 状态
     */
    private String status;
}
