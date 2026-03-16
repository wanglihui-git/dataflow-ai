package com.dataflow.ai.business.engine.dag;

import com.dataflow.ai.business.engine.exception.ExecutionException;
import com.dataflow.ai.domain.vo.Transform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * DAG构建器
 * 将Transform节点列表转换为DAG结构
 */
@Slf4j
@Component
public class DagBuilder {

    /**
     * 构建DAG
     */
    public Map<String, Node> build(List<Transform> transforms) {
        if (transforms == null || transforms.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 创建节点
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (Transform transform : transforms) {
            if (transform.getNodeId() == null) {
                throw new ExecutionException("Transform nodeId is required");
            }
            Node node = Node.builder()
                    .id(transform.getNodeId())
                    .transform(transform)
                    .dependencies(new ArrayList<>())
                    .dependents(new ArrayList<>())
                    .dependencyIds(new HashSet<>())
                    .build();
            nodes.put(transform.getNodeId(), node);
        }

        // 构建依赖关系
        for (Transform transform : transforms) {
            String nodeId = transform.getNodeId();
            Node node = nodes.get(nodeId);

            if (transform.getDependsOn() != null && !transform.getDependsOn().isEmpty()) {
                for (String depId : transform.getDependsOn()) {
                    Node depNode = nodes.get(depId);
                    if (depNode == null) {
                        throw new ExecutionException("Dependency not found: " + depId + " for node: " + nodeId);
                    }
                    node.addDependency(depNode);
                    log.debug("Added dependency: {} -> {}", depId, nodeId);
                }
            } else {
                // 如果没有显式依赖，默认依赖前一个节点
                int index = transforms.indexOf(transform);
                if (index > 0) {
                    Transform prevTransform = transforms.get(index - 1);
                    String prevId = prevTransform.getNodeId();
                    Node prevNode = nodes.get(prevId);
                    node.addDependency(prevNode);
                    log.debug("Added implicit dependency: {} -> {}", prevId, nodeId);
                }
            }
        }

        // 检查循环依赖
        checkForCycle(nodes);

        log.info("DAG built successfully: nodes={}", nodes.size());
        return nodes;
    }

    /**
     * 检查循环依赖
     */
    private void checkForCycle(Map<String, Node> nodes) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        for (Node node : nodes.values()) {
            if (!visited.contains(node.getId())) {
                if (node.hasCycle(visiting, visited)) {
                    // 找到循环
                    throw new ExecutionException("Circular dependency detected in DAG");
                }
            }
        }

        log.debug("No circular dependencies detected in DAG");
    }

    /**
     * 获取拓扑排序后的节点列表
     * 使用Kahn算法
     */
    public List<Node> topologicalSort(Map<String, Node> nodes) {
        List<Node> sorted = new ArrayList<>();

        if (nodes.isEmpty()) {
            return sorted;
        }

        // 计算每个节点的入度
        Map<String, Integer> inDegree = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();

        for (Node node : nodes.values()) {
            inDegree.put(node.getId(), node.getInDegree());
            if (node.getInDegree() == 0) {
                queue.offer(node);
            }
        }

        // 拓扑排序
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            sorted.add(current);

            for (Node dependent : current.getDependents()) {
                int newInDegree = inDegree.get(dependent.getId()) - 1;
                inDegree.put(dependent.getId(), newInDegree);

                if (newInDegree == 0) {
                    queue.offer(dependent);
                }
            }
        }

        // 检查是否所有节点都被排序（检测循环依赖）
        if (sorted.size() != nodes.size()) {
            throw new ExecutionException("Circular dependency detected during topological sort");
        }

        log.debug("Topological sort completed: nodes={}", sorted.size());
        return sorted;
    }

    /**
     * 获取可并行执行的节点组
     */
    public List<List<Node>> getParallelGroup(Map<String, Node> nodes) {
        List<List<Node>> groups = new ArrayList<>();

        if (nodes.isEmpty()) {
            return groups;
        }

        List<Node> sorted = topologicalSort(nodes);

        // 按层级分组
        Map<Integer, List<Node>> levelMap = new HashMap<>();
        for (Node node : sorted) {
            int level = node.getLevel();
            levelMap.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
        }

        // 转换为列表并按层级排序
        int maxLevel = levelMap.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int level = 0; level <= maxLevel; level++) {
            List<Node> levelNodes = levelMap.get(level);
            if (levelNodes != null && !levelNodes.isEmpty()) {
                groups.add(levelNodes);
                log.debug("Parallel group {}: {} nodes", level, levelNodes.size());
            }
        }

        return groups;
    }

    /**
     * 验证DAG结构的有效性
     */
    public void validate(Map<String, Node> nodes) {
        if (nodes == null) {
            throw new IllegalArgumentException("Nodes cannot be null");
        }

        // 检查是否有节点
        if (nodes.isEmpty()) {
            return;  // 空DAG是有效的
        }

        // 检查每个节点的ID是否唯一
        Set<String> ids = new HashSet<>();
        for (Node node : nodes.values()) {
            if (node.getId() == null || node.getId().trim().isEmpty()) {
                throw new ExecutionException("Node ID cannot be null or empty");
            }
            if (ids.contains(node.getId())) {
                throw new ExecutionException("Duplicate node ID: " + node.getId());
            }
            ids.add(node.getId());
        }

        // 检查依赖引用的有效性
        for (Node node : nodes.values()) {
            for (Node dep : node.getDependencies()) {
                if (!nodes.containsKey(dep.getId())) {
                    throw new ExecutionException("Invalid dependency reference: " + dep.getId() +
                            " in node: " + node.getId());
                }
            }
        }

        // 检查循环依赖
        checkForCycle(nodes);

        log.info("DAG validation passed: nodes={}", nodes.size());
    }
}
