package com.dataflow.ai.business.engine.exception;

import com.dataflow.ai.domain.enums.DataSourceType;

/**
 * 源数据异常
 * 在从数据源读取数据时抛出
 */
public class SourceException extends ExecutionException {

    /**
     * 数据源ID
     */
    private final String dataSourceId;

    /**
     * 数据源类型
     */
    private final DataSourceType sourceType;

    /**
     * 仅包含错误消息的构造器。
     *
     * @param message 错误描述
     */
    public SourceException(String message) {
        super(message);
        this.dataSourceId = null;
        this.sourceType = null;
    }

    /**
     * 包含错误消息与根因的构造器。
     *
     * @param message 错误描述
     * @param cause   根因异常
     */
    public SourceException(String message, Throwable cause) {
        super(message, cause);
        this.dataSourceId = null;
        this.sourceType = null;
    }

    /**
     * 绑定执行上下文与数据源信息的构造器。
     *
     * @param executionId  执行运行 ID
     * @param pipelineId   Pipeline ID
     * @param dataSourceId 数据源 ID
     * @param sourceType   数据源类型
     * @param message      错误描述
     */
    public SourceException(String executionId, String pipelineId, String dataSourceId,
                            DataSourceType sourceType, String message) {
        super(executionId, pipelineId, "SOURCE_ERROR", message);
        this.dataSourceId = dataSourceId;
        this.sourceType = sourceType;
    }

    /**
     * 完整上下文并携带根因的构造器。
     *
     * @param executionId  执行运行 ID
     * @param pipelineId   Pipeline ID
     * @param dataSourceId 数据源 ID
     * @param sourceType   数据源类型
     * @param message      错误描述
     * @param cause        根因异常
     */
    public SourceException(String executionId, String pipelineId, String dataSourceId,
                            DataSourceType sourceType, String message, Throwable cause) {
        super(executionId, pipelineId, "SOURCE_ERROR", message, cause);
        this.dataSourceId = dataSourceId;
        this.sourceType = sourceType;
    }

    /**
     * @return 关联的数据源 ID
     */
    public String getDataSourceId() {
        return dataSourceId;
    }

    /**
     * @return 数据源类型
     */
    public DataSourceType getSourceType() {
        return sourceType;
    }

    /**
     * {@inheritDoc}
     * <p>附加数据源 ID 与类型信息。</p>
     */
    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[SOURCE_ERROR] ");
        if (getExecutionId() != null) {
            sb.append("Execution: ").append(getExecutionId());
        }
        if (dataSourceId != null) {
            sb.append(", DataSource: ").append(dataSourceId);
        }
        if (sourceType != null) {
            sb.append(", Type: ").append(sourceType);
        }
        sb.append(" - ").append(getMessage());
        return sb.toString();
    }

    /**
     * 连接失败异常
     */
    public static SourceException connectionFailed(String executionId, String pipelineId,
                                                    String dataSourceId, DataSourceType sourceType,
                                                    Throwable cause) {
        return new SourceException(executionId, pipelineId, dataSourceId, sourceType,
                "Failed to connect to data source", cause);
    }

    /**
     * 查询执行失败异常
     */
    public static SourceException queryFailed(String executionId, String pipelineId,
                                              String dataSourceId, DataSourceType sourceType,
                                              String query, Throwable cause) {
        return new SourceException(executionId, pipelineId, dataSourceId, sourceType,
                "Failed to execute query: " + query, cause);
    }

    /**
     * 数据读取失败异常
     */
    public static SourceException readFailed(String executionId, String pipelineId,
                                             String dataSourceId, DataSourceType sourceType,
                                             Throwable cause) {
        return new SourceException(executionId, pipelineId, dataSourceId, sourceType,
                "Failed to read data from source", cause);
    }

    /**
     * 数据源配置错误异常
     */
    public static SourceException configurationError(String executionId, String pipelineId,
                                                      String dataSourceId, DataSourceType sourceType,
                                                      String configError) {
        return new SourceException(executionId, pipelineId, dataSourceId, sourceType,
                "Invalid data source configuration: " + configError);
    }
}
