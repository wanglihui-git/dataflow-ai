package com.dataflow.ai.business.engine.transform.impl;

import com.dataflow.ai.business.engine.exception.TransformException;
import com.dataflow.ai.business.engine.transform.TransformProcessor;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.dto.TransformContext;
import com.dataflow.ai.domain.enums.TransformType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 脚本转换处理器
 * 使用脚本引擎（如JavaScript/Groovy）进行自定义转换
 */
@Slf4j
@Component
public class ScriptProcessor implements TransformProcessor {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * 处理数据批次，执行本转换节点的业务逻辑。
     *
     * @param batch   输入数据批次
     * @param context 转换上下文（含节点配置与共享状态）
     * @return 处理后的数据批次
     * @throws Exception 配置无效或处理失败时抛出
     */
    @Override
    public DataBatch process(DataBatch batch, TransformContext context) throws Exception {
        log.debug("Processing Script transform: nodeId={}, batchId={}",
                context.getTransform().getNodeId(), batch.getBatchId());

        // 获取脚本配置
        String script = (String) context.getConfigValue("script");
        String language = (String) context.getConfigValue("language", "javascript");
        String outputField = (String) context.getConfigValue("outputField");

        if (script == null || script.trim().isEmpty()) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.SCRIPT, "script is required");
        }

        // 获取脚本引擎
        ScriptEngine engine = scriptEngineManager.getEngineByName(language);
        if (engine == null) {
            throw TransformException.configurationError(
                    context.getExecutionId(), context.getPipelineId(),
                    context.getTransform().getNodeId(), context.getTransform().getName(),
                    TransformType.SCRIPT, "Script engine not found for language: " + language);
        }

        // 处理每个记录
        List<Record> processedRecords = new ArrayList<>();
        for (Record record : batch.getRecords()) {
            Record processedRecord = new Record();
            processedRecord.setId(record.getId());

            // 复制所有字段
            for (String field : record.getFieldNames()) {
                processedRecord.set(field, record.get(field));
            }

            try {
                // 将记录数据注入脚本引擎
                Bindings bindings = engine.createBindings();
                bindings.put("record", new ScriptRecordWrapper(record));
                bindings.put("data", record.getData());
                bindings.put("context", context);
                bindings.put("logger", log);

                // 执行脚本
                Object result = engine.eval(script, bindings);

                // 如果有指定输出字段，则设置结果
                if (outputField != null && !outputField.isEmpty()) {
                    processedRecord.set(outputField, result);
                } else if (result instanceof Map) {
                    // 如果结果是一个Map，合并到记录中
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                        processedRecord.set(entry.getKey(), entry.getValue());
                    }
                }

            } catch (ScriptException e) {
                log.error("Error executing script: nodeId={}, error={}",
                        context.getTransform().getNodeId(), e.getMessage(), e);
                throw TransformException.processingFailed(
                        context.getExecutionId(), context.getPipelineId(),
                        context.getTransform().getNodeId(), context.getTransform().getName(),
                        TransformType.SCRIPT, "Script execution failed: " + e.getMessage(), e);
            }

            processedRecords.add(processedRecord);
        }

        // 创建新的批次
        DataBatch result = DataBatch.builder()
                .batchId(batch.getBatchId() + "_scripted")
                .sequenceNumber(batch.getSequenceNumber())
                .records(processedRecords)
                .metadata(new java.util.HashMap<>(batch.getMetadata()))
                .lastBatch(batch.isLastBatch())
                .build();

        log.debug("Script transform completed: nodeId={}, inputRecords={}, outputRecords={}",
                context.getTransform().getNodeId(), batch.size(), result.size());

        return result;
    }

    /**
     * 返回本处理器支持的转换类型标识。
     *
     * @return 转换类型名称
     */
    @Override
    public String getSupportedType() {
        return "SCRIPT";
    }

    /**
     * 脚本记录包装器
     * 提供友好的API供脚本访问记录
     */
    public static class ScriptRecordWrapper {
        private final Record record;

        public ScriptRecordWrapper(Record record) {
            this.record = record;
        }

        public Object get(String field) {
            return record.get(field);
        }

        public void set(String field, Object value) {
            record.set(field, value);
        }

        public boolean has(String field) {
            return record.containsField(field);
        }

        public Map<String, Object> toMap() {
            return record.getData();
        }
    }
}
