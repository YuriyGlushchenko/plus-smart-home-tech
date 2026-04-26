package ru.yandex.practicum.analyzer;

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
import org.springframework.context.annotation.Primary;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Getter
@Setter
@Configuration
public class KafkaConfiguration {

    @Value("${kafka.bootstrap-server:localhost:9092}")
    private String bootstrapServer;

    @Value("${kafka.topics.snapshots}")
    private String snapshotsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    @Value("${kafka.offsetCommitCount:10}")
    private int offsetCommitCount;

    // Настройки Producer
    @Value("${kafka.producer.key-serializer:org.apache.kafka.common.serialization.StringSerializer}")
    private String keySerializer;

    @Value("${kafka.producer.value-serializer:ru.yandex.practicum.analyzer.serialization.AvroSerializer}")
    private String valueSerializer;


    @Value("${kafka.consumer.poll-timeout:1000ms}")
    private Duration pollTimeout;

    @Value("${kafka.consumer.max-poll-records:100}")
    private Integer maxPollRecords;

    @Value("${kafka.consumer.fetch-max-bytes:3072000}")
    private Integer fetchMaxBytes;

    @Value("${kafka.consumer.max-partition-fetch-bytes:307200}")
    private Integer maxPartitionFetchBytes;



    @Value("${kafka.consumer.snapshot.group-id:analyzer-snapshot-group}")
    private String snapshotGroupId;

    @Value("${kafka.consumer.snapshot.client-id:analyzer-snapshot-consumer}")
    private String snapshotClientId;

    @Value("${kafka.consumer.snapshot.value-deserializer:ru.yandex.practicum.analyzer.serialization.SensorsSnapshotDeserializer}")
    private String snapshotValueDeserializer;



    @Value("${kafka.consumer.hub-event.group-id:analyzer-hub-group}")
    private String hubEventGroupId;

    @Value("${kafka.consumer.hub-event.client-id:analyzer-hub-consumer}")
    private String hubEventClientId;

    @Value("${kafka.consumer.hub-event.value-deserializer:ru.yandex.practicum.analyzer.serialization.HubEventDeserializer}")
    private String hubEventValueDeserializer;



    @Value("${kafka.consumer.key-deserializer:org.apache.kafka.common.serialization.StringDeserializer}")
    private String keyDeserializer;


    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer() {
        Properties config = createBaseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, snapshotGroupId);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, snapshotClientId);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, snapshotValueDeserializer);

        KafkaConsumer<String, SensorsSnapshotAvro> consumer = new KafkaConsumer<>(config);
        consumer.subscribe(List.of(snapshotsTopic));

        return consumer;
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer() {
        Properties config = createBaseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, hubEventGroupId);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, hubEventClientId);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, hubEventValueDeserializer);

        return new KafkaConsumer<>(config);
//        consumer.subscribe(List.of(hubsTopic));

//        return consumer;
    }

    private Properties createBaseConsumerConfig() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes);
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);
        return config;
    }
}