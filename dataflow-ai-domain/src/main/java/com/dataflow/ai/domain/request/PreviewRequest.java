package com.dataflow.ai.domain.request;

import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 预览请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewRequest {

    /**
     * 数据源ID
     */
    private String dataSourceId;

    /**
     * 表名（可选）
     */
    private String tableName;

    /**
     * 查询语句（可选）
     */
    private String query;

    /**
     * 源配置
     */
    private SourceConfig source;

    /**
     * 转换节点列表
     */
    private List<Transform> transforms;

    /**
     * 样本大小
     */
    @Builder.Default
    private int sampleSize = 10;
}
