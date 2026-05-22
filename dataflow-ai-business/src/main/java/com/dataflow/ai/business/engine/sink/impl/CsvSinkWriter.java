package com.dataflow.ai.business.engine.sink.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SinkException;
import com.dataflow.ai.business.engine.sink.SinkWriter;
import com.dataflow.ai.business.repository.DataSourceRepository;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * CSV目标写入器
 * 支持向CSV文件写入数据
 */
@Slf4j
@Component
public class CsvSinkWriter implements SinkWriter {

    @Resource
    private DataSourceRepository dataSourceRepository;

    @Resource
    private EncryptionService encryptionService;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 将数据批次写入目标存储。
     *
     * @param batch      待写入批次
     * @param sinkConfig 目标配置
     * @param context    执行上下文
     * @return 实际写入的记录数
     * @throws Exception 连接或写入失败时抛出
     */
    @Override
    public long write(DataBatch batch, SinkConfig sinkConfig, ExecutionContext context) throws Exception {
        String dataSourceId = sinkConfig.getDataSourceId();

        log.info("Writing to CSV sink: dataSourceId={}, batchSize={}", dataSourceId, batch.size());

        // 获取数据源配置
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new SinkException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, DataSourceType.CSV, null, null,
                        "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String filePath = (String) config.get("filePath");
        char delimiter = parseDelimiter((String) config.get("delimiter"));
        boolean includeHeader = Boolean.TRUE.equals(config.getOrDefault("includeHeader", true));
        String encoding = (String) config.getOrDefault("encoding", "UTF-8");
        boolean appendMode = Boolean.TRUE.equals(config.getOrDefault("appendMode", false));
        boolean writeBom = Boolean.TRUE.equals(config.getOrDefault("writeBom", false));

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SinkException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.CSV, null, null,
                    "CSV file path is required");
        }

        log.debug("Writing CSV file: path={}, delimiter={}, includeHeader={}, encoding={}, appendMode={}",
                filePath, delimiter, includeHeader, encoding, appendMode);

        Path path = Paths.get(filePath);

        // 创建父目录
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        // 获取所有字段名
        Set<String> allFields = new LinkedHashSet<>();
        for (Record record : batch.getRecords()) {
            allFields.addAll(record.getFieldNames());
        }
        List<String> headers = new ArrayList<>(allFields);
        Collections.sort(headers);

        long writtenCount = 0;

        try (OutputStream outputStream = new BufferedOutputStream(
                Files.newOutputStream(path,
                        appendMode ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE),
                DEFAULT_BUFFER_SIZE);
             Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(delimiter))) {

            // 写入BOM（用于Excel正确识别UTF-8编码）
            if (writeBom && !appendMode && !Files.exists(path)) {
                writer.write('\uFEFF');
            }

            // 写入表头
            if (includeHeader && (!appendMode || !Files.exists(path) || Files.size(path) == 0)) {
                printer.printRecord(headers);
            }

            // 写入数据
            for (Record record : batch.getRecords()) {
                List<Object> values = new ArrayList<>();
                for (String field : headers) {
                    Object value = record.get(field);
                    values.add(formatValue(value));
                }
                printer.printRecord(values);
                writtenCount++;
            }

            printer.flush();

            log.debug("CSV write completed: path={}, recordsWritten={}", filePath, writtenCount);
            return writtenCount;

        } catch (IOException e) {
            log.error("Error writing to CSV: path={}, error={}", filePath, e.getMessage(), e);
            throw SinkException.writeFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.CSV, null, null, e);
        }
    }

    /**
     * 返回本写入器支持的目标类型标识。
     *
     * @return 类型名称字符串
     */
    @Override
    public String getSupportedType() {
        return "CSV";
    }

    /**
     * 测试目标数据源是否可连接。
     *
     * @param dataSource 数据源实体
     * @return 连接成功返回 true
     */
    @Override
    public boolean testConnection(DataSource dataSource) {
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String filePath = (String) config.get("filePath");

        if (filePath == null) {
            return false;
        }

        Path path = Paths.get(filePath);
        return Files.exists(path.getParent()) && Files.isWritable(path.getParent());
    }

    @Override
    public void initialize(SinkConfig sinkConfig, ExecutionContext context) throws Exception {
        // 实现可选的初始化逻辑，如创建备份文件等
        log.debug("CSV sink initialization: dataSourceId={}", sinkConfig.getDataSourceId());
    }

    /**
     * 解析分隔符
     */
    private char parseDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            return CSVFormat.DEFAULT.getDelimiter();
        }

        switch (delimiter.toLowerCase()) {
            case "\\t":
            case "tab":
                return '\t';
            case ",":
            case "comma":
                return ',';
            case ";":
            case "semicolon":
                return ';';
            case "|":
            case "pipe":
                return '|';
            default:
                if (delimiter.length() == 1) {
                    return delimiter.charAt(0);
                }
                return CSVFormat.DEFAULT.getDelimiter();
        }
    }

    /**
     * 格式化值
     */
    private Object formatValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
