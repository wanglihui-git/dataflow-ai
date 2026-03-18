package com.dataflow.ai.business.engine.source.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SourceException;
import com.dataflow.ai.business.engine.source.SourceReader;
import com.dataflow.ai.business.service.DataSourceService;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.infrastructure.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka源读取器
 * 支持从Kafka主题读取消息
 */
@Slf4j
@Component
public class KafkaSourceReader implements SourceReader {

    @Resource
    private DataSourceService dataSourceService;

    @Resource
    private EncryptionService encryptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_PREVIEW_SIZE = 100;
    private static final int DEFAULT_POLL_TIMEOUT_MS = 5000;
    private static final int DEFAULT_MAX_RECORDS = 10000;

    @Override
    public List<Record> read(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, ExecutionContext context)
            throws Exception {
        String dataSourceId = sourceConfig.getDataSourceId();
        DataSourceType type = sourceConfig.getType();

        log.info("Reading from Kafka source: dataSourceId={}, type={}", dataSourceId, type);

        // 获取数据源配置
        DataSource dataSource = dataSourceService.findById(dataSourceId)
                .orElseThrow(() -> new SourceException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, type, "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String bootstrapServers = (String) config.get("bootstrapServers");
        String topic = (String) config.get("topic");
        String groupId = (String) config.getOrDefault("groupId", "dataflow-ai-consumer");
        String offsetStrategy = (String) config.getOrDefault("offsetStrategy", "latest");

        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            throw new SourceException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, "Kafka bootstrap servers are required");
        }

        if (topic == null || topic.trim().isEmpty()) {
            throw new SourceException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, "Kafka topic is required");
        }

        int maxRecords = sourceConfig.getParams() != null
                ? (int) sourceConfig.getParams().getOrDefault("maxRecords", DEFAULT_MAX_RECORDS)
                : DEFAULT_MAX_RECORDS;

        log.debug("Reading from Kafka: bootstrapServers={}, topic={}, groupId={}, offsetStrategy={}, maxRecords={}",
                bootstrapServers, topic, groupId, offsetStrategy, maxRecords);

        // 创建Kafka消费者配置
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetStrategy.toLowerCase());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        List<Record> records = new ArrayList<>();
        Consumer<String, String> consumer = null;

        try {
            // 创建消费者
            consumer = new KafkaConsumer<>(props);

            // 订阅主题
            consumer.subscribe(Collections.singletonList(topic));

            AtomicInteger recordsRead = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();
            long timeout = context.getSharedState() != null
                    ? (long) context.getSharedState().getOrDefault("readTimeout", 60000L)
                    : 60000L;

            // 读取消息
            while (recordsRead.get() < maxRecords && !context.isCancelled()) {
                // 检查超时
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.debug("Kafka read timeout reached: runId={}, recordsRead={}",
                            context.getRunId(), recordsRead.get());
                    break;
                }

                // 拉取消息
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));

                if (consumerRecords.isEmpty()) {
                    // 没有消息，继续轮询
                    continue;
                }

                // 处理消息
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    if (recordsRead.get() >= maxRecords) {
                        break;
                    }

                    Record dataRecord = parseKafkaRecord(record);
                    records.add(dataRecord);

                    // 更新上下文
                    context.incrementRecordsProcessed(1);
                    recordsRead.incrementAndGet();
                }

                // 手动提交偏移量
                consumer.commitSync();
            }

            log.info("Kafka read completed: runId={}, recordsRead={}", context.getRunId(), records.size());
            return records;

        } catch (Exception e) {
            log.error("Error reading from Kafka: topic={}, error={}", topic, e.getMessage(), e);
            throw SourceException.readFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, type, e);
        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (Exception e) {
                    log.warn("Error closing Kafka consumer: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public DataSourceType getSupportedType() {
        return DataSourceType.KAFKA;
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        Map<String, Object> config = encryptionService.decrypt(dataSource.getConnectionConfig());
        String bootstrapServers = (String) config.get("bootstrapServers");

        if (bootstrapServers == null) {
            return false;
        }

        // 简单的连接测试：尝试创建消费者
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            // 如果能创建消费者，说明连接成功
            return true;
        } catch (Exception e) {
            log.error("Kafka connection test failed: bootstrapServers={}, error={}", bootstrapServers, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Record> preview(com.dataflow.ai.domain.vo.SourceConfig sourceConfig, int sampleSize)
            throws Exception {
        if (sampleSize <= 0) {
            sampleSize = DEFAULT_PREVIEW_SIZE;
        }

        // 创建临时上下文并限制读取数量
        Map<String, Object> params = sourceConfig.getParams() != null
                ? new HashMap<>(sourceConfig.getParams())
                : new HashMap<>();
        params.put("maxRecords", sampleSize);
        sourceConfig.setParams(params);

        ExecutionContext tempContext = ExecutionContext.builder()
                .runId("preview_" + System.currentTimeMillis())
                .build();

        // 读取数据
        List<Record> records = read(sourceConfig, tempContext);

        return records;
    }

    /**
     * 解析Kafka记录为Record
     */
    private Record parseKafkaRecord(ConsumerRecord<String, String> record) {
        Record dataRecord = new Record();
        dataRecord.setId(UUID.randomUUID().toString());

        // 添加Kafka元数据
        dataRecord.set("kafka_topic", record.topic());
        dataRecord.set("kafka_partition", record.partition());
        dataRecord.set("kafka_offset", record.offset());
        dataRecord.set("kafka_key", record.key());

        // 尝试解析消息体为JSON
        String value = record.value();
        if (value != null && !value.isEmpty()) {
            try {
                // 检查是否为JSON对象
                if (value.trim().startsWith("{")) {
                    Map<String, Object> jsonMap = objectMapper.readValue(value, Map.class);
                    for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                        dataRecord.set(entry.getKey(), entry.getValue());
                    }
                } else if (value.trim().startsWith("[")) {
                    // JSON数组，将数组作为单个字段
                    dataRecord.set("value", value);
                } else {
                    // 纯文本
                    dataRecord.set("value", value);
                }
            } catch (Exception e) {
                // 不是有效的JSON，作为纯文本处理
                dataRecord.set("value", value);
            }
        }

        return dataRecord;
    }
}
