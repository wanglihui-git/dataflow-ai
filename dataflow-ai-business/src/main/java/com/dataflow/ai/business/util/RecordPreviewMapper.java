package com.dataflow.ai.business.util;

import com.dataflow.ai.domain.dto.Record;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 {@link Record} 列表转为预览 API 的 columns + rows 结构
 */
public final class RecordPreviewMapper {

    private RecordPreviewMapper() {
    }

    public static Map<String, Object> toPreviewMap(List<Record> records) {
        List<String> columns = new ArrayList<>();
        Set<String> columnSet = new LinkedHashSet<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Record record : records) {
            for (String field : record.getFieldNames()) {
                if (columnSet.add(field)) {
                    columns.add(field);
                }
            }
            rows.add(record.getData());
        }

        return Map.of(
                "columns", columns,
                "rows", rows,
                "rowCount", rows.size()
        );
    }
}
