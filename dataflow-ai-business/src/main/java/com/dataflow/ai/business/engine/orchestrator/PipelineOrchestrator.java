package com.dataflow.ai.business.engine.orchestrator;

import com.dataflow.ai.business.engine.dag.DagExecutor;
import com.dataflow.ai.business.engine.util.TransformDagSupport;
import com.dataflow.ai.business.service.ExecutionService;
import com.dataflow.ai.business.engine.exception.ExecutionException;
import com.dataflow.ai.business.engine.metrics.ExecutionMetricsCollector;
import com.dataflow.ai.business.engine.sink.SinkWriter;
import com.dataflow.ai.business.engine.sink.SinkWriterFactory;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.business.engine.transform.TransformProcessorFactory;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.entity.Pipeline;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.domain.vo.SourceConfig;
import com.dataflow.ai.domain.vo.Transform;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pipeline编排器
 * 协调Source → Transform → Sink的完整执行流程
 */
@Slf4j
@Component
public class PipelineOrchestrator {

    @Resource
    private SourceReaderFactory sourceReaderFactory;

    @Resource
    private TransformProcessorFactory transformProcessorFactory;

    @Resource
    private SinkWriterFactory sinkWriterFactory;

    @Resource
    private DagExecutor dagExecutor;

    @Resource
    private ExecutionMetricsCollector metricsCollector;

    @Resource
    @Lazy
    private ExecutionService executionService;

    /**
     * 默认批次大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 执行Pipeline
     */
    public ExecutionResult execute(ExecutionContext context) {
        ExecutionResult result = ExecutionResult.builder()
                .startTime(context.getStartTime())
                .build();

        try {
            context.setPhase(ExecutionContext.ExecutionPhase.INIT);
            log.info("Starting pipeline execution: runId={}, pipelineName={}",
                    context.getRunId(), context.getPipeline().getName());

            // 初始化指标收集
            metricsCollector.initialize(context);
            executionService.appendExecutionLog(context.getRunId(), "INIT", "Execution started");

            // Phase 1: 读取源数据
            List<Record> allRecords = executeSourcePhase(context, result);
            if (context.isCancelled()) {
                result = ExecutionResult.cancelled();
                result.setStartTime(context.getStartTime());
                result.calculateDuration();
                return result;
            }

            // Phase 2: 执行转换
            List<Record> transformedRecords = executeTransformPhase(context, allRecords, result);
            if (context.isCancelled()) {
                result = ExecutionResult.cancelled();
                result.setStartTime(context.getStartTime());
                result.calculateDuration();
                return result;
            }

            // Phase 3: 写入目标
            executeSinkPhase(context, transformedRecords, result);
            if (context.isCancelled()) {
                result = ExecutionResult.cancelled();
                result.setStartTime(context.getStartTime());
                result.calculateDuration();
                return result;
            }

            // 执行成功
            result = ExecutionResult.success();
            result.setStartTime(context.getStartTime());
            result.setRecordsProcessed(context.getRecordsProcessed().get());
            result.setRecordsFailed(context.getRecordsFailed().get());
            result.setBatchesProcessed(context.getBatchesProcessed().get());
            result.calculateDuration();
            result.setMetrics(metricsCollector.collectFinalMetrics(context));
            metricsCollector.getDataSourceMetrics().forEach(result::addDataSourceMetrics);

            context.markCompleted();
            log.info("Pipeline execution completed successfully: runId={}, recordsProcessed={}, durationMs={}",
                    context.getRunId(), result.getRecordsProcessed(), result.getDurationMs());

        } catch (ExecutionException e) {
            context.markFailed();
            result = ExecutionResult.failure(e.getMessage(), e);
            result.setStartTime(context.getStartTime());
            result.setRecordsProcessed(context.getRecordsProcessed().get());
            result.setRecordsFailed(context.getRecordsFailed().get());
            result.calculateDuration();
            result.setMetrics(metricsCollector.collectFinalMetrics(context));
            log.error("Pipeline execution failed: runId={}, error={}", context.getRunId(), e.getDetailedMessage(), e);
        } catch (InterruptedException e) {
            context.markCancelled();
            result = ExecutionResult.cancelled();
            result.setStartTime(context.getStartTime());
            result.calculateDuration();
            log.warn("Pipeline execution cancelled: runId={}", context.getRunId());
        } catch (Exception e) {
            context.markFailed();
            result = ExecutionResult.failure("Unexpected error: " + e.getMessage(), e);
            result.setStartTime(context.getStartTime());
            result.setRecordsProcessed(context.getRecordsProcessed().get());
            result.setRecordsFailed(context.getRecordsFailed().get());
            result.calculateDuration();
            log.error("Unexpected error during pipeline execution: runId={}", context.getRunId(), e);
        }

        return result;
    }

    /**
     * 执行源数据读取阶段
     */
    private List<Record> executeSourcePhase(ExecutionContext context, ExecutionResult result)
            throws Exception {
        context.setPhase(ExecutionContext.ExecutionPhase.SOURCE);

        SourceConfig sourceConfig = context.getPipeline().getSource();
        if (sourceConfig == null) {
            throw new ExecutionException(context.getRunId(), context.getPipeline().getId(),
                    "Source configuration is missing");
        }

        Optional<DataSource> dataSourceOpt = sourceReaderFactory.getDataSourceById(sourceConfig.getDataSourceId());
        if (dataSourceOpt.isEmpty()) {
            throw new ExecutionException(context.getRunId(), context.getPipeline().getId(),
                    "Data source not found: " + sourceConfig.getDataSourceId());
        }

        DataSource dataSource = dataSourceOpt.get();
        log.info("Reading from source: dataSourceId={}, type={}, tableName={}, query={}",
                sourceConfig.getDataSourceId(), dataSource.getType(),
                sourceConfig.getTableName(), sourceConfig.getQuery());

        metricsCollector.startSourceMetric(context, sourceConfig.getDataSourceId(), dataSource.getType().name());

        SourceReader reader = sourceReaderFactory.createReader(dataSource);
        List<Record> records = reader.read(sourceConfig, context);

        metricsCollector.endSourceMetric(context, sourceConfig.getDataSourceId(), records.size());

        log.info("Source phase completed: runId={}, recordsRead={}", context.getRunId(), records.size());
        return records;
    }

    /**
     * 执行转换阶段
     */
    private List<Record> executeTransformPhase(ExecutionContext context, List<Record> inputRecords,
                                               ExecutionResult result) throws Exception {
        context.setPhase(ExecutionContext.ExecutionPhase.TRANSFORM);

        List<Transform> transforms = context.getPipeline().getTransforms();
        if (transforms == null || transforms.isEmpty()) {
            log.debug("No transforms configured, skipping transform phase: runId={}", context.getRunId());
            return inputRecords;
        }

        log.info("Executing {} transform(s): runId={}", transforms.size(), context.getRunId());

        // 构建DAG并执行转换
        List<Transform> sortedTransforms = dagExecutor.buildAndExecute(context, transforms, inputRecords);

        // 应用转换
        List<Record> transformedRecords = new ArrayList<>(inputRecords);
        for (Transform transform : sortedTransforms) {
            if (context.isCancelled() || executionService.isCancelRequested(context.getRunId())) {
                context.markCancelled();
                break;
            }

            TransformDagSupport.prepareJoinRightData(transform, context.getSharedState());
            TransformProcessor processor = transformProcessorFactory.createProcessor(transform.getType());
            TransformContext transformContext = TransformContext.builder()
                    .transform(transform)
                    .sharedState(context.getSharedState())
                    .executionId(context.getRunId())
                    .pipelineId(context.getPipeline().getId())
                    .build();

            // 分批处理
            int batchSize = DEFAULT_BATCH_SIZE;
            int batchNumber = 0;
            for (int i = 0; i < transformedRecords.size(); i += batchSize) {
                if (context.isCancelled()) {
                    break;
                }

                int endIndex = Math.min(i + batchSize, transformedRecords.size());
                List<Record> batchRecords = transformedRecords.subList(i, endIndex);
                DataBatch batch = DataBatch.builder()
                        .batchId(context.getRunId() + "_batch_" + batchNumber)
                        .sequenceNumber(batchNumber)
                        .records(batchRecords)
                        .build();

                transformContext.setBatchNumber(batchNumber);
                transformContext.setFirstBatch(batchNumber == 0);
                transformContext.setLastBatch(endIndex >= transformedRecords.size());

                metricsCollector.startTransformMetric(context, transform.getNodeId());

                DataBatch transformedBatch = processor.process(batch, transformContext);

                metricsCollector.endTransformMetric(context, transform.getNodeId(), transformedBatch.size());

                // 替换批次中的记录
                for (int j = 0; j < batchRecords.size(); j++) {
                    transformedRecords.set(i + j, transformedBatch.getRecord(j));
                }

                context.incrementBatchesProcessed();
                batchNumber++;
            }

            TransformDagSupport.saveNodeOutput(context.getSharedState(), transform.getNodeId(), transformedRecords);
            executionService.appendExecutionLog(context.getRunId(), "TRANSFORM",
                    "Completed node " + transform.getNodeId() + " type=" + transform.getType());
            log.debug("Transform completed: nodeId={}, type={}, recordsProcessed={}",
                    transform.getNodeId(), transform.getType(), transformedRecords.size());
        }

        context.incrementRecordsProcessed(transformedRecords.size());
        log.info("Transform phase completed: runId={}, finalRecords={}", context.getRunId(), transformedRecords.size());
        return transformedRecords;
    }

    /**
     * 执行目标写入阶段
     */
    private void executeSinkPhase(ExecutionContext context, List<Record> records, ExecutionResult result)
            throws Exception {
        context.setPhase(ExecutionContext.ExecutionPhase.SINK);

        SinkConfig sinkConfig = context.getPipeline().getSink();
        if (sinkConfig == null) {
            throw new ExecutionException(context.getRunId(), context.getPipeline().getId(),
                    "Sink configuration is missing");
        }

        Optional<DataSource> dataSourceOpt = sinkWriterFactory.getDataSourceById(sinkConfig.getDataSourceId());
        if (dataSourceOpt.isEmpty()) {
            throw new ExecutionException(context.getRunId(), context.getPipeline().getId(),
                    "Data source not found: " + sinkConfig.getDataSourceId());
        }

        DataSource dataSource = dataSourceOpt.get();
        log.info("Writing to sink: dataSourceId={}, type={}, tableName={}, writeMode={}",
                sinkConfig.getDataSourceId(), dataSource.getType(),
                sinkConfig.getTableName(), sinkConfig.getWriteMode());

        metricsCollector.startSinkMetric(context, sinkConfig.getDataSourceId(), dataSource.getType().name());

        SinkWriter writer = sinkWriterFactory.createWriter(dataSource);

        // 分批写入
        int batchSize = sinkConfig.getBatchSize() != null ? sinkConfig.getBatchSize() : DEFAULT_BATCH_SIZE;
        int batchNumber = 0;
        for (int i = 0; i < records.size(); i += batchSize) {
            if (context.isCancelled()) {
                break;
            }

            int endIndex = Math.min(i + batchSize, records.size());
            List<Record> batchRecords = records.subList(i, endIndex);
            DataBatch batch = DataBatch.builder()
                    .batchId(context.getRunId() + "_batch_" + batchNumber)
                    .sequenceNumber(batchNumber)
                    .records(batchRecords)
                    .build();

            long writtenCount = writer.write(batch, sinkConfig, context);
            context.incrementRecordsProcessed(writtenCount);

            batchNumber++;
        }

        metricsCollector.endSinkMetric(context, sinkConfig.getDataSourceId(), context.getRecordsProcessed().get());

        log.info("Sink phase completed: runId={}, recordsWritten={}", context.getRunId(), context.getRecordsProcessed().get());
    }
}
