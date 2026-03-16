package com.dataflow.ai.business.engine.sink.impl;

import com.dataflow.ai.business.engine.orchestrator.ExecutionContext;
import com.dataflow.ai.business.engine.exception.SinkException;
import com.dataflow.ai.business.engine.sink.SinkWriter;
import com.dataflow.ai.business.engine.sink.SinkWriterFactory;
import com.dataflow.ai.domain.dto.DataBatch;
import com.dataflow.ai.domain.dto.Record;
import com.dataflow.ai.domain.entity.DataSource;
import com.dataflow.ai.domain.enums.DataSourceType;
import com.dataflow.ai.domain.vo.SinkConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;

/**
 * Kafka目标写入器
 * 支持向Kafka主题写入消息
 */
@Slf4j
@Component
public class KafkaSinkWriter implements SinkWriter {

    @Resource
    private SinkWriterFactory sinkWriterFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public long write(DataBatch batch, SinkConfig sinkConfig, ExecutionContext context) throws Exception {
        String dataSourceId = sinkConfig.getDataSourceId();

        log.info("Writing to Kafka sink: dataSourceId={}, batchSize={}", dataSourceId, batch.size());

        // 获取数据源配置
        DataSource dataSource = sinkWriterFactory.getDataSourceById(dataSourceId)
                .orElseThrow(() -> new SinkException(
                        context.getRunId(), context.getPipeline().getId(),
                        dataSourceId, DataSourceType.KAFKA, null, null,
                        "Data source not found"));

        // 获取解密后的连接配置
        Map<String, Object> config = sinkWriterFactory.getDecryptedConnectionConfig(dataSource);
        String bootstrapServers = (String) config.get("bootstrapServers");
        String topic = (String) config.get("topic");
        String keyField = (String) config.get("keyField");
        String partitionField = (String) config.get("partitionField");

        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            throw new SinkException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.KAFKA, null, null,
                    "Kafka bootstrap servers are required");
        }

        if (topic == null || topic.trim().isEmpty()) {
            throw new SinkException(
                    context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.KAFKA, null, null,
                    "Kafka topic is required");
        }

        log.debug("Writing to Kafka: bootstrapServers={}, topic={}, keyField={}", bootstrapServers, topic, keyField);

        // 创建Kafka生产者配置
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        long writtenCount = 0;
        KafkaProducer<String, String> producer = null;

        try {
            producer = new KafkaProducer<>(props);

            // 发送消息
            for (Record record : batch.getRecords()) {
                // 序列化消息体
                String messageBody = serializeRecord(record);

                // 获取消息键
                String messageKey = null;
                if (keyField != null && record.containsField(keyField)) {
                    messageKey = record.get(keyField) != null ? record.get(keyField).toString() : null;
                }

                // 获取分区
                Integer partition = null;
                if (partitionField != null && record.containsField(partitionField)) {
                    Object partitionValue = record.get(partitionField);
                    if (partitionValue instanceof Number) {
                        partition = ((Number) partitionValue).intValue();
                    }
                }

                // 创建生产者记录
                ProducerRecord<String, String> producerRecord;
                if (partition != null) {
                    producerRecord = new ProducerRecord<>(topic, partition, messageKey, messageBody);
                } else {
                    producerRecord = new ProducerRecord<>(topic, messageKey, messageBody);
                }

                // 发送消息
                producer.send(producerRecord, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send message to Kafka: topic={}, key={}",
                                topic, messageKey, exception);
                    }
                });

                writtenCount++;
            }

            producer.flush();

            log.debug("Kafka write completed: topic={}, recordsSent={}", topic, writtenCount);
            return writtenCount;

        } catch (Exception e) {
            log.error("Error writing to Kafka: topic={}, error={}", topic, e.getMessage(), e);
            throw SinkException.writeFailed(context.getRunId(), context.getPipeline().getId(),
                    dataSourceId, DataSourceType.KAFKA, topic, null, e);
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (Exception e) {
                    log.warn("Error closing Kafka producer: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public String getSupportedType() {
        return "KAFKA";
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        Map<String, Object> config = sinkWriterFactory.getDecryptedConnectionConfig(dataSource);
        String bootstrapServers = (String) config.get("bootstrapServers");

        if (bootstrapServers == null) {
            return false;
        }

        // 简单的连接测试：尝试创建生产者
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            // 如果能创建生产者，说明连接成功
            return true;
        } catch (Exception e) {
            log.error("Kafka connection test failed: bootstrapServers={}, error={}", bootstrapServers, e.getMessage());
            return false;
        }
    }

    /**
     * 序列化记录为JSON字符串
     */
    private String serializeRecord(Record record) throws Exception {
        try {
            return objectMapper.writeValueAsString(record.getData());
        } catch (Exception e) {
            log.error("Failed to serialize record: recordId={}", record.getId(), e);
            throw e;
        }
    }
}
