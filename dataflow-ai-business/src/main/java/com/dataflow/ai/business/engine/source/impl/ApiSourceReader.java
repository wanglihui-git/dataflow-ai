package com.dataflow.ai.business.engine.source.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SourceException;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API源读取器
 * 支持从REST API读取JSON数据
 */
@Slf4j
@Component
public class ApiSourceReader implements SourceReader {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    @Resource
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_PREVIEW_SIZE = 100;

    @Override
    public List<Record> read(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, ExecutionContext context)
            throws Exception {
        String dataSourceId = sourceConfig.getDataSourceId();
        DataSourceType type = sourceConfig.getType();

        log.info("Reading from API source: dataSourceId={}, type={}", dataSourceId, type);

        // 获取数据源配置
        DataSource dataSource = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new SourceException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, type, "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) config.get("headers");
        Map<String, Object> params = sourceConfig.getParams();

        // 构建请求URL
        if (params != null && !params.isEmpty()) {
            url = buildUrlWithParams(url, params);
        }

        log.debug("Executing API request: url={}, method={}", url, method);

        try {
            // 创建请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            // 创建请求实体
            HttpEntity<?> requestEntity = new HttpEntity<>(httpHeaders);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method),
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SourceException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, type,
                        "API request failed with status: " + response.getStatusCode());
            }

            // 解析响应
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("API response body is empty: dataSourceId={}", dataSourceId);
                return new ArrayList<>();
            }

            // 解析JSON并转换为Record列表
            List<Record> records = parseJsonResponse(responseBody, config);

            // 更新上下文
            context.incrementRecordsProcessed(records.size());

            log.info("API read completed: runId={}, recordsRead={}", context.getRunId(), records.size());
            return records;

        } catch (Exception e) {
            log.error("Error reading from API: dataSourceId={}, error={}", dataSourceId, e.getMessage(), e);
            throw SourceException.readFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, e);
        }
    }

    @Override
    public DataSourceType getSupportedType() {
        return DataSourceType.API;
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = (String) config.get("url");

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("API connection test failed: url={}, error={}", url, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Record> preview(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, int sampleSize)
            throws Exception {
        if (sampleSize <= 0) {
            sampleSize = DEFAULT_PREVIEW_SIZE;
        }

        // 读取数据
        List<Record> records = read(sourceConfig,
                ExecutionContext.builder().runId("preview_" + System.currentTimeMillis()).build());

        // 限制返回数量
        if (records.size() > sampleSize) {
            records = records.subList(0, sampleSize);
        }

        return records;
    }

    /**
     * 构建带参数的URL
     */
    private String buildUrlWithParams(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("?")) {
            sb.append("?");
        } else if (!url.endsWith("&") && !url.endsWith("?")) {
            sb.append("&");
        }

        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return sb.toString();
    }

    /**
     * 解析JSON响应并转换为Record列表
     */
    private List<Record> parseJsonResponse(String responseBody, Map<String, Object> config) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        List<Record> records = new ArrayList<>();

        // 检查是否有数据路径配置
        String dataPath = (String) config.get("dataPath");

        JsonNode dataNode = rootNode;
        if (dataPath != null && !dataPath.isEmpty()) {
            // 支持点号分隔的路径，如 "data.items"
            for (String pathSegment : dataPath.split("\\.")) {
                if (dataNode != null && dataNode.has(pathSegment)) {
                    dataNode = dataNode.get(pathSegment);
                } else {
                    dataNode = null;
                    break;
                }
            }
        }

        if (dataNode == null) {
            log.warn("Data path not found in response: dataPath={}", dataPath);
            return records;
        }

        // 处理数组或单个对象
        if (dataNode.isArray()) {
            for (JsonNode item : dataNode) {
                Record record = parseJsonNodeToRecord(item);
                records.add(record);
            }
        } else if (dataNode.isObject()) {
            Record record = parseJsonNodeToRecord(dataNode);
            records.add(record);
        }

        return records;
    }

    /**
     * 解析JsonNode为Record
     */
    private Record parseJsonNodeToRecord(JsonNode node) {
        Record record = new Record();
        record.setId(UUID.randomUUID().toString());

        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode valueNode = entry.getValue();

            Object value = parseJsonValue(valueNode);
            record.set(key, value);
        });

        return record;
    }

    /**
     * 解析JSON节点值
     */
    private Object parseJsonValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(parseJsonValue(item));
            }
            return list;
        } else if (node.isObject()) {
            Map<String, Object> map = new java.util.HashMap<>();
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), parseJsonValue(entry.getValue()));
            });
            return map;
        }
        return node.toString();
    }
}
