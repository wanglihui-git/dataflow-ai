package com.dataflow.ai.business.engine.preview;

import com.dataflow.ai.business.engine.dag.DagExecutor;
import com.dataflow.ai.business.engine.util.TransformDagSupport;
import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.business.engine.transform.TransformProcessorFactory;
import com.dataflow.ai.business.util.RecordPreviewMapper;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.domain.vo.Transform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pipeline 转换预览执行器。
 * <p>从源端采样数据，经 DAG 拓扑排序后依次执行 Transform 节点，不写入 Sink，用于 UI 预览。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelinePreviewExecutor {

    private static final int DEFAULT_BATCH_SIZE = 500;

    private final SourceReaderFactory sourceReaderFactory;
    private final TransformProcessorFactory transformProcessorFactory;
    private final DagExecutor dagExecutor;

    /**
     * 执行 Pipeline 预览：读源采样 → DAG 排序 → 逐节点转换 → 返回预览 Map。
     *
     * @param pipeline   Pipeline 实体
     * @param sampleSize 源端最大采样条数
     * @return 含预览记录、pipelineId、sampleSize、transformCount 的结果 Map
     * @throws Exception 源不存在或转换失败时抛出
     */
    public Map<String, Object> preview(Pipeline pipeline, int sampleSize) throws Exception {
        // 步骤1：校验并加载数据源
        if (pipeline.getSource() == null) {
            throw new IllegalArgumentException("Pipeline 未配置数据源");
        }
        SourceConfig sourceConfig = pipeline.getSource();
        Optional<DataSource> dataSourceOpt = sourceReaderFactory.getDataSourceById(sourceConfig.getDataSourceId());
        if (dataSourceOpt.isEmpty()) {
            throw new IllegalArgumentException("数据源不存在: " + sourceConfig.getDataSourceId());
        }

        SourceReader reader = sourceReaderFactory.createReader(dataSourceOpt.get());
        List<Record> records = reader.preview(sourceConfig, sampleSize);

        // 步骤2：若有 Transform，经 DAG 排序后分批应用
        List<Transform> transforms = pipeline.getTransforms();
        if (transforms != null && !transforms.isEmpty()) {
            ExecutionContext ctx = ExecutionContext.builder()
                    .runId("preview_" + System.currentTimeMillis())
                    .pipeline(pipeline)
                    .build();
            List<Transform> sorted = dagExecutor.buildAndExecute(ctx, transforms, records);
            records = applyTransforms(sorted, records, pipeline.getId(), ctx);
        }

        Map<String, Object> result = new HashMap<>(RecordPreviewMapper.toPreviewMap(records));
        result.put("pipelineId", pipeline.getId());
        result.put("sampleSize", sampleSize);
        result.put("transformCount", transforms == null ? 0 : transforms.size());
        return result;
    }

    /**
     * 按拓扑序依次执行各 Transform 节点，JOIN 等节点通过共享状态传递中间结果。
     *
     * @param transforms 已排序的转换节点列表
     * @param inputRecords 源端采样记录
     * @param pipelineId   Pipeline ID
     * @param ctx            预览用执行上下文
     * @return 经过全部节点后的记录列表
     * @throws Exception 某节点处理失败时抛出
     */
    private List<Record> applyTransforms(List<Transform> transforms, List<Record> inputRecords,
                                         String pipelineId, ExecutionContext ctx) throws Exception {
        List<Record> transformed = new ArrayList<>(inputRecords);
        for (Transform transform : transforms) {
            // 步骤2.1：准备 JOIN 右表等共享数据
            TransformDagSupport.prepareJoinRightData(transform, ctx.getSharedState());
            TransformProcessor processor = transformProcessorFactory.createProcessor(transform.getType());
            TransformContext transformContext = TransformContext.builder()
                    .transform(transform)
                    .sharedState(ctx.getSharedState())
                    .executionId(ctx.getRunId())
                    .pipelineId(pipelineId)
                    .build();

            // 步骤2.2：按固定批次大小调用处理器，避免单次内存过大
            int batchSize = DEFAULT_BATCH_SIZE;
            List<Record> next = new ArrayList<>(transformed.size());
            for (int i = 0; i < transformed.size(); i += batchSize) {
                int end = Math.min(i + batchSize, transformed.size());
                List<Record> slice = new ArrayList<>(transformed.subList(i, end));
                DataBatch batch = DataBatch.builder()
                        .batchId(ctx.getRunId() + "_preview_" + transform.getNodeId())
                        .sequenceNumber(i / batchSize)
                        .records(slice)
                        .lastBatch(end >= transformed.size())
                        .build();
                transformContext.setBatchNumber(i / batchSize);
                transformContext.setFirstBatch(i == 0);
                transformContext.setLastBatch(end >= transformed.size());
                DataBatch out = processor.process(batch, transformContext);
                next.addAll(out.getRecords());
            }
            transformed = next;
            TransformDagSupport.saveNodeOutput(ctx.getSharedState(), transform.getNodeId(), transformed);
        }
        return transformed;
    }
}
