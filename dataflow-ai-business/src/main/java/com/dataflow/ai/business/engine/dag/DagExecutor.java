package com.dataflow.ai.business.engine.dag;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.vo.Transform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * DAG 执行器。
 * <p>负责构建、校验 DAG，并按拓扑序或分层并行方式驱动转换节点执行。</p>
 */
@Slf4j
@Component
public class DagExecutor {

    private final DagBuilder dagBuilder;
    private final ExecutorService executorService;

    /**
     * 注入 DAG 构建器并初始化固定大小线程池，用于同层节点并行执行。
     *
     * @param dagBuilder DAG 构建器
     */
    public DagExecutor(DagBuilder dagBuilder) {
        this.dagBuilder = dagBuilder;
        // 使用固定线程池进行并行执行
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("dag-executor-" + t.getId());
                    t.setDaemon(false);
                    return t;
                });
    }

    /**
     * 构建 DAG、校验后返回拓扑排序的 Transform 列表（不实际改写记录数据）。
     *
     * @param context      执行上下文
     * @param transforms   原始转换节点列表
     * @param inputRecords 输入记录（用于兼容签名，当前排序逻辑不依赖其内容）
     * @return 拓扑排序后的 Transform 列表
     * @throws Exception DAG 构建或校验失败时抛出
     */
    public List<Transform> buildAndExecute(ExecutionContext context, List<Transform> transforms,
                                            List<Record> inputRecords) throws Exception {
        if (transforms == null || transforms.isEmpty()) {
            return transforms;
        }

        // 构建DAG
        Map<String, Node> nodes = dagBuilder.build(transforms);

        // 验证DAG
        dagBuilder.validate(nodes);

        // 获取拓扑排序
        List<Node> sortedNodes = dagBuilder.topologicalSort(nodes);

        // 转换为Transform列表
        List<Transform> sortedTransforms = sortedNodes.stream()
                .map(Node::getTransform)
                .collect(Collectors.toList());

        log.debug("DAG executed: runId={}, nodes={}, sortedTransforms={}",
                context.getRunId(), nodes.size(), sortedTransforms.size());

        return sortedTransforms;
    }

    /**
     * 按并行分组逐层执行 DAG 节点（当前实现仅标记节点已执行，数据变换由编排层完成）。
     *
     * @param context      执行上下文
     * @param transforms   转换节点列表
     * @param inputRecords 输入记录
     * @return 处理后的记录列表
     * @throws Exception 并行执行过程中发生未捕获异常时抛出
     */
    public List<Record> executeParallel(ExecutionContext context, List<Transform> transforms,
                                         List<Record> inputRecords) throws Exception {
        if (transforms == null || transforms.isEmpty()) {
            return inputRecords;
        }

        // 构建DAG
        Map<String, Node> nodes = dagBuilder.build(transforms);

        // 获取并行组
        List<List<Node>> groups = dagBuilder.getParallelGroup(nodes);

        // 步骤3：按拓扑层级分组，同层节点在线程池中并行执行
        List<Record> currentRecords = new ArrayList<>(inputRecords);
        for (List<Node> group : groups) {
            if (context.isCancelled()) {
                break;
            }

            // 步骤3.1：为当前层每个节点提交异步任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Node node : group) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 这里应该调用TransformProcessor来处理数据
                        // 由于TransformProcessor的复杂性，这里只是标记节点已执行
                        node.setExecuted(true);
                        log.debug("Node executed: nodeId={}, level={}",
                                node.getId(), node.getLevel());
                    } catch (Exception e) {
                        log.error("Error executing node: nodeId={}", node.getId(), e);
                        throw new RuntimeException(e);
                    }
                }, executorService);
                futures.add(future);
            }

            // 步骤3.2：阻塞等待本层全部节点完成后再进入下一层
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.debug("Group completed: runId={}, groupSize={}", context.getRunId(), group.size());
        }

        log.debug("DAG parallel execution completed: runId={}, groups={}, totalRecords={}",
                context.getRunId(), groups.size(), currentRecords.size());

        return currentRecords;
    }

    /**
     * 统计 DAG 节点数、边数与最大深度。
     *
     * @param transforms 转换节点列表
     * @return 含 nodeCount、edgeCount、maxDepth 的统计 Map
     */
    public Map<String, Object> getDagStats(List<Transform> transforms) {
        if (transforms == null || transforms.isEmpty()) {
            return Map.of(
                    "nodeCount", 0,
                    "edgeCount", 0,
                    "maxDepth", 0
            );
        }

        Map<String, Node> nodes = dagBuilder.build(transforms);

        int nodeCount = nodes.size();
        int edgeCount = nodes.values().stream()
                .mapToInt(node -> node.getDependencies().size())
                .sum();

        int maxDepth = nodes.values().stream()
                .mapToInt(Node::getLevel)
                .max()
                .orElse(0);

        return Map.of(
                "nodeCount", nodeCount,
                "edgeCount", edgeCount,
                "maxDepth", maxDepth
        );
    }

    /**
     * 关闭内部线程池，应用停机时调用。
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
