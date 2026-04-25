package ru.yandex.practicum.aggregator;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Getter
@Setter
@Configuration
public class KafkaConfiguration {


    @Value("${kafka.bootstrap-server:localhost:9092}")
    private String bootstrapServer;

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.snapshots}")
    private String snapshotsTopic;

    @Value("${kafka.producer.key-serializer:org.apache.kafka.common.serialization.StringSerializer}")
    private String keySerializer;

    @Value("${kafka.producer.value-serializer:ru.yandex.practicum.aggregator.serialization.AvroSerializer}")
    private String valueSerializer;

    @Value("${kafka.consumer.poll-timeout:1000ms}")
    private Duration pollTimeout;


    @Value("${kafka.consumer.group-id:aggregator-group}")
    private String consumerGroupId;

    @Value("${kafka.consumer.client-id:aggregator-consumer}")
    private String consumerClientId;

    @Value("${kafka.consumer.max-poll-records:100}")
    private Integer maxPollRecords;

    @Value("${kafka.consumer.fetch-max-bytes:3072000}")
    private Integer fetchMaxBytes;

    @Value("${kafka.consumer.max-partition-fetch-bytes:307200}")
    private Integer maxPartitionFetchBytes;

    @Value("${kafka.consumer.enable-auto-commit:false}")
    private Boolean enableAutoCommit;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.consumer.key-deserializer:org.apache.kafka.common.serialization.StringDeserializer}")
    private String keyDeserializer;

    @Value("${kafka.consumer.value-deserializer:ru.yandex.practicum.aggregator.serialization.SensorEventDeserializer}")
    private String valueDeserializer;

    @Value("${kafka.consumer.topics:}")
    private List<String> consumerTopics;


    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        return new KafkaProducer<>(config);
    }

    @Bean
    public KafkaConsumer<String, SpecificRecordBase> kafkaConsumer() {
        Properties config = new Properties();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerClientId);

        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);

        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes);
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);

        return new KafkaConsumer<>(config);
    }
}