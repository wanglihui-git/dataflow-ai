package com.dataflow.ai.business.engine.preview;

import com.dataflow.ai.business.engine.dag.DagExecutor;
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
 * Pipeline 转换预览：采样源数据并执行 transforms（不写 Sink）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelinePreviewExecutor {

    private static final int DEFAULT_BATCH_SIZE = 500;

    private final SourceReaderFactory sourceReaderFactory;
    private final TransformProcessorFactory transformProcessorFactory;
    private final DagExecutor dagExecutor;

    public Map<String, Object> preview(Pipeline pipeline, int sampleSize) throws Exception {
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

    private List<Record> applyTransforms(List<Transform> transforms, List<Record> inputRecords,
                                         String pipelineId, ExecutionContext ctx) throws Exception {
        List<Record> transformed = new ArrayList<>(inputRecords);
        for (Transform transform : transforms) {
            TransformProcessor processor = transformProcessorFactory.createProcessor(transform.getType());
            TransformContext transformContext = TransformContext.builder()
                    .transform(transform)
                    .sharedState(ctx.getSharedState())
                    .executionId(ctx.getRunId())
                    .pipelineId(pipelineId)
                    .build();

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
        }
        return transformed;
    }
}
