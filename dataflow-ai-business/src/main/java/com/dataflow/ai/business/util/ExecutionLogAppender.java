package com.dataflow.ai.business.util;

import com.dataflow.ai.domain.entity.ExecutionRun;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行日志 JSON 追加工具。
 * <p>在 {@link ExecutionRun#getExecutionLog()} 的 {@code entries} 数组中追加带时间戳的阶段日志。</p>
 */
public final class ExecutionLogAppender {

    private static final String ENTRIES_KEY = "entries";

    private ExecutionLogAppender() {
    }

    /**
     * 向执行记录追加一条日志条目（原地修改 executionLog Map）。
     *
     * @param run     执行记录实体
     * @param phase   阶段标识（如 INIT、TRANSFORM、ERROR）
     * @param message 日志正文
     */
    public static void append(ExecutionRun run, String phase, String message) {
        Map<String, Object> log = run.getExecutionLog();
        if (log == null) {
            log = new HashMap<>();
            run.setExecutionLog(log);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) log.computeIfAbsent(
                ENTRIES_KEY, k -> new ArrayList<>());
        entries.add(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "phase", phase,
                "message", message
        ));
    }

    /**
     * 读取执行记录中的日志条目列表。
     *
     * @param run 执行记录
     * @return 条目列表，无日志或格式异常时返回空列表
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getEntries(ExecutionRun run) {
        if (run.getExecutionLog() == null) {
            return List.of();
        }
        Object entries = run.getExecutionLog().get(ENTRIES_KEY);
        if (entries instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }
}
