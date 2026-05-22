package com.dataflow.ai.business.engine.sink.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SinkException;
import com.dataflow.ai.business.engine.sink.SinkWriter;
import com.dataflow.ai.business.engine.sink.SinkWriterFactory;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API目标写入器
 * 支持向REST API写入数据
 */
@Slf4j
@Component
public class ApiSinkWriter implements SinkWriter {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    @Resource
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        log.info("Writing to API sink: dataSourceId={}, batchSize={}", dataSourceId, batch.size());

        // 获取数据源配置
        DataSource dataSource = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new SinkException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, DataSourceType.API, null, null,
                        "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "POST");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) config.get("headers");
        boolean sendAsArray = Boolean.TRUE.equals(config.getOrDefault("sendAsArray", false));

        log.debug("Executing API write: url={}, method={}, sendAsArray={}", url, method, sendAsArray);

        long writtenCount = 0;

        try {
            // 创建请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
            Object requestBody;
            if (sendAsArray) {
                // 将整个批次作为数组发送
                List<Map<String, Object>> recordsAsMaps = new ArrayList<>();
                for (Record record : batch.getRecords()) {
                    recordsAsMaps.add(record.getData());
                }
                requestBody = recordsAsMaps;
            } else {
                // 每条记录单独发送
                requestBody = batch.getRecords().get(0).getData();
            }

            // 创建请求实体
            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, httpHeaders);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method),
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new SinkException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, DataSourceType.API, null, null,
                        "API request failed with status: " + response.getStatusCode());
            }

            writtenCount = sendAsArray ? batch.size() : 1;

            log.debug("API write completed: recordsWritten={}", writtenCount);
            return writtenCount;

        } catch (Exception e) {
            log.error("Error writing to API: dataSourceId={}, error={}", dataSourceId, e.getMessage(), e);
            throw SinkException.writeFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.API, null, null, e);
        }
    }

    /**
     * 返回本写入器支持的目标类型标识。
     *
     * @return 类型名称字符串
     */
    @Override
    public String getSupportedType() {
        return "API";
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
        String url = (String) config.get("url");

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("API connection test failed: url={}, error={}", url, e.getMessage());
            return false;
        }
    }
}
