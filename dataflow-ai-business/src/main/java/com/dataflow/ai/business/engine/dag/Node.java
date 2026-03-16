package com.dataflow.ai.business.engine.dag;

import com.dataflow.ai.domain.vo.Transform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * DAG节点表示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 转换对象
     */
    private Transform transform;

    /**
     * 依赖节点（入边）
     */
    @Builder.Default
    private List<Node> dependencies = new ArrayList<>();

    /**
     * 依赖节点ID集合（用于快速查找）
     */
    @Builder.Default
    private Set<String> dependencyIds = new java.util.HashSet<>();

    /**
     * 依赖此节点的节点（出边）
     */
    @Builder.Default
    private List<Node> dependents = new ArrayList<>();

    /**
     * 入度
     */
    @Builder.Default
    private int inDegree = 0;

    /**
     * 是否已访问
     */
    @Builder.Default
    private boolean visited = false;

    /**
     * 是否已执行
     */
    @Builder.Default
    private boolean executed = false;

    /**
     * 添加依赖节点
     */
    public void addDependency(Node node) {
        if (node != null && !dependencyIds.contains(node.getId())) {
            dependencies.add(node);
            dependencyIds.add(node.getId());
            node.getDependents().add(this);
            inDegree = dependencies.size();
        }
    }

    /**
     * 添加多个依赖节点
     */
    public void addDependencies(List<Node> nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                addDependency(node);
            }
        }
    }

    /**
     * 检查是否有循环依赖
     */
    public boolean hasCycle(Set<String> visiting, Set<String> visited) {
        if (visiting.contains(id)) {
            return true;  // 发现循环
        }

        if (visited.contains(id)) {
            return false;  // 已访问过，不是循环
        }

        visiting.add(id);

        for (Node dep : dependencies) {
            if (dep.hasCycle(visiting, visited)) {
                return true;
            }
        }

        visiting.remove(id);
        visited.add(id);
        return false;
    }

    /**
     * 获取所有依赖节点（递归）
     */
    public List<Node> getAllDependencies() {
        List<Node> all = new ArrayList<>();
        collectAllDependencies(all);
        return all;
    }

    /**
     * 收集所有依赖节点
     */
    private void collectAllDependencies(List<Node> collector) {
        for (Node dep : dependencies) {
            if (!collector.contains(dep)) {
                collector.add(dep);
                dep.collectAllDependencies(collector);
            }
        }
    }

    /**
     * 获取所有依赖此节点的节点（递归）
     */
    public List<Node> getAllDependents() {
        List<Node> all = new ArrayList<>();
        collectAllDependents(all);
        return all;
    }

    /**
     * 收集所有依赖此节点的节点
     */
    private void collectAllDependents(List<Node> collector) {
        for (Node dep : dependents) {
            if (!collector.contains(dep)) {
                collector.add(dep);
                dep.collectAllDependents(collector);
            }
        }
    }

    /**
     * 判断是否为根节点（无依赖）
     */
    public boolean isRoot() {
        return dependencies.isEmpty();
    }

    /**
     * 判断是否为叶子节点（无依赖者）
     */
    public boolean isLeaf() {
        return dependents.isEmpty();
    }

    /**
     * 获取层级（从根节点开始计算）
     */
    public int getLevel() {
        if (dependencies.isEmpty()) {
            return 0;
        }
        int maxLevel = 0;
        for (Node dep : dependencies) {
            maxLevel = Math.max(maxLevel, dep.getLevel());
        }
        return maxLevel + 1;
    }

    /**
     * 重置访问状态
     */
    public void resetVisited() {
        this.visited = false;
        for (Node dep : dependencies) {
            dep.resetVisited();
        }
    }

    /**
     * 重置执行状态
     */
    public void resetExecuted() {
        this.executed = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id != null && id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
