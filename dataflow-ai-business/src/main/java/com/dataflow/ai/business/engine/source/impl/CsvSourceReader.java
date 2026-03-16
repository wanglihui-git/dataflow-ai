package com.dataflow.ai.business.engine.source.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SourceException;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.engine.source.SourceReaderFactory;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * CSV源读取器
 * 支持从CSV文件读取数据
 */
@Slf4j
@Component
public class CsvSourceReader implements SourceReader {

    @Resource
    private SourceReaderFactory sourceReaderFactory;

    private static final int DEFAULT_PREVIEW_SIZE = 100;
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    @Override
    public List<Record> read(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, ExecutionContext context)
            throws Exception {
        String dataSourceId = sourceConfig.getDataSourceId();
        DataSourceType type = sourceConfig.getType();

        log.info("Reading from CSV source: dataSourceId={}, type={}", dataSourceId, type);

        // 获取数据源配置
        DataSource dataSource = sourceReaderFactory.getDataSourceById(dataSourceId)
                .orElseThrow(() -> new SourceException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, type, "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = sourceReaderFactory.getDecryptedConnectionConfig(dataSource);
        String filePath = (String) config.get("filePath");

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SourceException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, "CSV file path is required");
        }

        // 解析CSV配置
        char delimiter = parseDelimiter((String) config.get("delimiter"));
        boolean hasHeader = Boolean.TRUE.equals(config.get("hasHeader"));
        String encoding = (String) config.getOrDefault("encoding", "UTF-8");

        log.debug("Reading CSV file: path={}, delimiter={}, hasHeader={}, encoding={}",
                filePath, delimiter, hasHeader, encoding);

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new SourceException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, "CSV file not found: " + filePath);
        }

        List<Record> records = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(path.toFile()), DEFAULT_BUFFER_SIZE);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
             CSVParser parser = CSVFormat.DEFAULT
                     .withDelimiter(delimiter)
                     .withHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .parse(reader);

            // 获取表头
            headers = new ArrayList<>(parser.getHeaderNames());

            log.debug("CSV headers: {}", headers);

            // 读取记录
            for (CSVRecord csvRecord : parser) {
                // 检查是否已取消
                if (context.isCancelled()) {
                    log.warn("Execution cancelled while reading from CSV: runId={}", context.getRunId());
                    break;
                }

                Record record = new Record();
                record.setId(UUID.randomUUID().toString());

                // 将CSV记录转换为Record
                for (String header : headers) {
                    if (csvRecord.isMapped(header)) {
                        String value = csvRecord.get(header);
                        record.set(header, parseValue(value));
                    }
                }

                records.add(record);

                // 更新上下文
                context.incrementRecordsProcessed(1);
            }

        } catch (IOException e) {
            log.error("Error reading from CSV: filePath={}, error={}", filePath, e.getMessage(), e);
            throw SourceException.readFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, e);
        }

        log.info("CSV read completed: runId={}, recordsRead={}", context.getRunId(), records.size());
        return records;
    }

    @Override
    public String getSupportedType() {
        return "CSV";
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        Map<String, Object> config = sourceReaderFactory.getDecryptedConnectionConfig(dataSource);
        String filePath = (String) config.get("filePath");

        if (filePath == null) {
            return false;
        }

        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isReadable(path);
    }

    @Override
    public List<Record> preview(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, int sampleSize)
            throws Exception {
        if (sampleSize <= 0) {
            sampleSize = DEFAULT_PREVIEW_SIZE;
        }

        // 创建临时上下文
        ExecutionContext tempContext = ExecutionContext.builder()
                .runId("preview_" + System.currentTimeMillis())
                .build();

        // 读取数据
        List<Record> records = read(sourceConfig, tempContext);

        // 限制返回数量
        if (records.size() > sampleSize) {
            records = records.subList(0, sampleSize);
        }

        return records;
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
     * 解析值（自动识别类型）
     */
    private Object parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // 尝试解析为布尔值
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        // 尝试解析为整数
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            // 不是整数，继续尝试其他类型
        }

        // 尝试解析为长整型
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e1) {
            // 不是长整型，继续尝试其他类型
        }

        // 尝试解析为双精度浮点数
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e1) {
            // 不是双精度浮点数，返回字符串
        }

        return value;
    }
}
