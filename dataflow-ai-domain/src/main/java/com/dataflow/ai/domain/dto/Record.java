package com.dataflow.ai.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通用数据记录
 * 使用 Map-based 结构支持灵活的模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    /**
     * 记录ID（可选）
     */
    private String id;

    /**
     * 数据内容
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * 元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 获取字段值
     */
    public Object get(String field) {
        return data.get(field);
    }

    /**
     * 设置字段值
     */
    public void set(String field, Object value) {
        data.put(field, value);
    }

    /**
     * 检查字段是否存在
     */
    public boolean containsField(String field) {
        return data.containsKey(field);
    }

    /**
     * 获取所有字段名
     */
    public java.util.Set<String> getFieldNames() {
        return data.keySet();
    }

    /**
     * 获取字段数量
     */
    public int getFieldCount() {
        return data.size();
    }

    /**
     * 复制记录
     */
    public Record copy() {
        Record copy = new Record();
        copy.setId(this.id);
        copy.setData(new HashMap<>(this.data));
        copy.setMetadata(new HashMap<>(this.metadata));
        return copy;
    }

    /**
     * 合并另一个记录的字段
     */
    public void merge(Record other, boolean overwrite) {
        for (Map.Entry<String, Object> entry : other.getData().entrySet()) {
            if (overwrite || !this.data.containsKey(entry.getKey())) {
                this.data.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 重命名字段
     */
    public void renameField(String oldName, String newName) {
        if (data.containsKey(oldName)) {
            data.put(newName, data.remove(oldName));
        }
    }

    /**
     * 删除字段
     */
    public void removeField(String field) {
        data.remove(field);
    }

    /**
     * 将 data 字段手工拼接为简易 JSON 字符串（非完整 JSON 库实现）。
     *
     * @return JSON 风格字符串
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            // 按 JSON 基本类型规则拼接字面量
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else {
                // 其他类型退化为字符串
                sb.append("\"").append(value.toString()).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return Objects.equals(data, record.data);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
