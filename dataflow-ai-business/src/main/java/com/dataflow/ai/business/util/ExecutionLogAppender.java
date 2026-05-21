package com.dataflow.ai.business.util;

import com.dataflow.ai.domain.entity.ExecutionRun;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行日志 JSON 追加（entries 列表）
 */
public final class ExecutionLogAppender {

    private static final String ENTRIES_KEY = "entries";

    private ExecutionLogAppender() {
    }

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
