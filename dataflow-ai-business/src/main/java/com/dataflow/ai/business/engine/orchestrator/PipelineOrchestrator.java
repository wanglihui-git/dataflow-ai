package com.dataflow.ai.business.engine.orchestrator;

import com.dataflow.ai.business.config.EngineProperties;
import com.dataflow.ai.business.engine.dag.DagBuilder;
import com.dataflow.ai.business.engine.dag.DagExecutor;
import com.dataflow.ai.business.engine.dag.Node;
import com.dataflow.ai.business.engine.retry.ExecutionRetryHelper;
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
import java.util.Map;
import java.util.Optional;

/**
 * Pipeline 执行编排器。
 * <p>按 Source → Transform → Sink 三阶段协调读写与转换，集成 DAG 排序、指标收集与指数退避重试。</p>
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

    @Resource
    private ExecutionRetryHelper executionRetryHelper;

    @Resource
    private EngineProperties engineProperties;

    @Resource
    private DagBuilder dagBuilder;

    /**
     * 默认批次大小
     */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 执行完整 Pipeline 流程直至成功、失败或取消。
     *
     * @param context 执行上下文（含 runId、Pipeline 配置及取消标志）
     * @return 含状态、指标、处理行数等信息的执行结果
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
     * 源数据读取阶段：解析 SourceConfig、创建 Reader 并在重试策略下全量读取。
     *
     * @param context 执行上下文
     * @param result  累积结果对象（本阶段主要写入指标）
     * @return 读取到的全部记录
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
        List<Record> records = executionRetryHelper.execute(
                context.getRunId(), "source-read", context.getPipeline().getSchedule(),
                () -> reader.read(sourceConfig, context));

        metricsCollector.endSourceMetric(context, sourceConfig.getDataSourceId(), records.size());

        log.info("Source phase completed: runId={}, recordsRead={}", context.getRunId(), records.size());
        return records;
    }

    /**
     * 转换阶段：构建 DAG、按拓扑或并行层级顺序逐节点分批处理记录。
     *
     * @param context      执行上下文（含 sharedState 供 JOIN 等节点使用）
     * @param inputRecords 源阶段输出记录
     * @param result       累积结果
     * @return 全部转换完成后的记录列表
     */
    private List<Record> executeTransformPhase(ExecutionContext context, List<Record> inputRecords,
                                               ExecutionResult result) throws Exception {
        context.setPhase(ExecutionContext.ExecutionPhase.TRANSFORM);

        List<Transform> transforms = context.getPipeline().getTransforms();
        if (transforms == null || transforms.isEmpty()) {
            log.debug("No transforms configured, skipping transform phase: runId={}", context.getRunId());
            return inputRecords;
        }

        log.info("Executing {} transform(s): runId={}, parallelDag={}",
                transforms.size(), context.getRunId(), engineProperties.isParallelDagEnabled());

        // DagExecutor 可预执行并行层；主循环仍按序写回同一记录集以保证正确性
        dagExecutor.buildAndExecute(context, transforms, inputRecords);

        List<Record> transformedRecords = new ArrayList<>(inputRecords);
        Map<String, Node> nodes = dagBuilder.build(transforms);
        dagBuilder.validate(nodes);

        // 确定节点执行顺序：并行模式按层级扁平化，否则拓扑排序
        List<Transform> orderedTransforms = engineProperties.isParallelDagEnabled()
                ? flattenParallelGroups(dagBuilder.getParallelGroup(nodes))
                : dagBuilder.topologicalSort(nodes).stream().map(Node::getTransform).toList();

        for (Transform transform : orderedTransforms) {
            if (context.isCancelled() || executionService.isCancelRequested(context.getRunId())) {
                context.markCancelled();
                break;
            }

            // JOIN 节点需从 sharedState 注入右表数据
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

                // 将本批转换结果写回原列表对应下标
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
     * 目标写入阶段：按 Sink 配置的 batchSize 分批写入，写操作带重试。
     *
     * @param context 执行上下文
     * @param records 转换后的全部记录
     * @param result  累积结果
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

            final DataBatch writeBatch = batch;
            long writtenCount = executionRetryHelper.execute(
                    context.getRunId(), "sink-write", context.getPipeline().getSchedule(),
                    () -> writer.write(writeBatch, sinkConfig, context));
            context.incrementRecordsProcessed(writtenCount);

            batchNumber++;
        }

        metricsCollector.endSinkMetric(context, sinkConfig.getDataSourceId(), context.getRecordsProcessed().get());

        log.info("Sink phase completed: runId={}, recordsWritten={}", context.getRunId(), context.getRecordsProcessed().get());
    }

    /**
     * 将 DAG 并行层级列表扁平化为 Transform 执行顺序。
     *
     * @param groups 每层节点列表
     * @return 按层、按节点顺序排列的 Transform 列表
     */
    private List<Transform> flattenParallelGroups(List<List<Node>> groups) {
        List<Transform> ordered = new ArrayList<>();
        for (List<Node> group : groups) {
            for (Node node : group) {
                ordered.add(node.getTransform());
            }
        }
        return ordered;
    }
}
