package com.dataflow.ai.business.engine.exception;

import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.SinkConfig;

/**
 * 目标写入异常
 * 在向目标写入数据时抛出
 */
public class SinkException extends ExecutionException {

    /**
     * 目标数据源ID
     */
    private final String dataSourceId;

    /**
     * 目标数据源类型
     */
    private final DataSourceType sinkType;

    /**
     * 目标表名
     */
    private final String tableName;

    /**
     * 写入模式
     */
    private final SinkConfig.WriteMode writeMode;

    /**
     * 批次序号
     */
    private final Integer batchNumber;

    /**
     * 仅包含错误消息的构造器。
     *
     * @param message 错误描述
     */
    public SinkException(String message) {
        super(message);
        this.dataSourceId = null;
        this.sinkType = null;
        this.tableName = null;
        this.writeMode = null;
        this.batchNumber = null;
    }

    /**
     * 包含错误消息与根因的构造器。
     *
     * @param message 错误描述
     * @param cause   根因异常
     */
    public SinkException(String message, Throwable cause) {
        super(message, cause);
        this.dataSourceId = null;
        this.sinkType = null;
        this.tableName = null;
        this.writeMode = null;
        this.batchNumber = null;
    }

    /**
     * 绑定执行上下文与 Sink 配置的构造器。
     *
     * @param executionId  执行运行 ID
     * @param pipelineId   Pipeline ID
     * @param dataSourceId 目标数据源 ID
     * @param sinkType     目标类型
     * @param tableName    目标表名
     * @param writeMode    写入模式
     * @param message      错误描述
     */
    public SinkException(String executionId, String pipelineId, String dataSourceId,
                         DataSourceType sinkType, String tableName,
                         SinkConfig.WriteMode writeMode, String message) {
        super(executionId, pipelineId, "SINK_ERROR", message);
        this.dataSourceId = dataSourceId;
        this.sinkType = sinkType;
        this.tableName = tableName;
        this.writeMode = writeMode;
        this.batchNumber = null;
    }

    /**
     * 完整 Sink 上下文并携带根因的构造器。
     *
     * @param executionId  执行运行 ID
     * @param pipelineId   Pipeline ID
     * @param dataSourceId 目标数据源 ID
     * @param sinkType     目标类型
     * @param tableName    目标表名
     * @param writeMode    写入模式
     * @param message      错误描述
     * @param cause        根因异常
     */
    public SinkException(String executionId, String pipelineId, String dataSourceId,
                         DataSourceType sinkType, String tableName,
                         SinkConfig.WriteMode writeMode, String message, Throwable cause) {
        super(executionId, pipelineId, "SINK_ERROR", message, cause);
        this.dataSourceId = dataSourceId;
        this.sinkType = sinkType;
        this.tableName = tableName;
        this.writeMode = writeMode;
        this.batchNumber = null;
    }

    private SinkException(String executionId, String pipelineId, String dataSourceId,
                           DataSourceType sinkType, String tableName,
                           SinkConfig.WriteMode writeMode, Integer batchNumber,
                           String message, Throwable cause) {
        super(executionId, pipelineId, "SINK_ERROR", message, cause);
        this.dataSourceId = dataSourceId;
        this.sinkType = sinkType;
        this.tableName = tableName;
        this.writeMode = writeMode;
        this.batchNumber = batchNumber;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public DataSourceType getSinkType() {
        return sinkType;
    }

    public String getTableName() {
        return tableName;
    }

    public SinkConfig.WriteMode getWriteMode() {
        return writeMode;
    }

    public Integer getBatchNumber() {
        return batchNumber;
    }

    /**
     * 创建带批次信息的异常
     */
    public SinkException withBatch(int batchNumber) {
        return new SinkException(getExecutionId(), getPipelineId(), dataSourceId,
                sinkType, tableName, writeMode, batchNumber, getMessage(), getCause());
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[SINK_ERROR] ");
        if (getExecutionId() != null) {
            sb.append("Execution: ").append(getExecutionId());
        }
        if (dataSourceId != null) {
            sb.append(", DataSource: ").append(dataSourceId);
        }
        if (tableName != null) {
            sb.append(", Table: ").append(tableName);
        }
        if (writeMode != null) {
            sb.append(", Mode: ").append(writeMode);
        }
        if (batchNumber != null) {
            sb.append(", Batch: ").append(batchNumber);
        }
        sb.append(" - ").append(getMessage());
        return sb.toString();
    }

    /**
     * 连接失败异常
     */
    public static SinkException connectionFailed(String executionId, String pipelineId,
                                                  String dataSourceId, DataSourceType sinkType,
                                                  Throwable cause) {
        return new SinkException(executionId, pipelineId, dataSourceId, sinkType,
                null, null, "Failed to connect to sink data source", cause);
    }

    /**
     * 写入失败异常
     */
    public static SinkException writeFailed(String executionId, String pipelineId,
                                             String dataSourceId, DataSourceType sinkType,
                                             String tableName, SinkConfig.WriteMode writeMode,
                                             Throwable cause) {
        return new SinkException(executionId, pipelineId, dataSourceId, sinkType,
                tableName, writeMode, "Failed to write data to sink", cause);
    }

    /**
     * 批量写入失败异常
     */
    public static SinkException batchWriteFailed(String executionId, String pipelineId,
                                                  String dataSourceId, DataSourceType sinkType,
                                                  String tableName, SinkConfig.WriteMode writeMode,
                                                  int batchNumber, int batchSize, Throwable cause) {
        String message = String.format("Failed to write batch %d (size: %d)", batchNumber, batchSize);
        return new SinkException(executionId, pipelineId, dataSourceId, sinkType,
                tableName, writeMode, batchNumber, message, cause);
    }

    /**
     * 表不存在异常
     */
    public static SinkException tableNotFound(String executionId, String pipelineId,
                                               String dataSourceId, DataSourceType sinkType,
                                               String tableName) {
        return new SinkException(executionId, pipelineId, dataSourceId, sinkType,
                tableName, null, "Target table does not exist: " + tableName);
    }

    /**
     * 表结构不匹配异常
     */
    public static SinkException schemaMismatch(String executionId, String pipelineId,
                                               String dataSourceId, DataSourceType sinkType,
                                               String tableName, String reason) {
        return new SinkException(executionId, pipelineId, dataSourceId, sinkType,
                tableName, null, "Schema mismatch: " + reason);
    }

    /**
     * 写入模式不支持异常
     */
    public static SinkException unsupportedWriteMode(String executionId, String pipelineId,
                                                     String dataSourceId, DataSourceType sinkType,
                                                     SinkConfig.WriteMode writeMode) {
        return new SinkException(executionId, pipelineId, dataSourceId, sinkType,
                null, writeMode, "Write mode not supported for this sink type: " + writeMode);
    }
}
