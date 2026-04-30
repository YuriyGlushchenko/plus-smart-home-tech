package ru.yandex.practicum.aggregator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private String bootstrapServer = "localhost:9092";
    private Topics topics = new Topics();
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();

    @Data
    public static class Topics {
        private String sensors = "telemetry.sensors.v1";
        private String snapshots = "telemetry.snapshots.v1";
    }

    @Data
    public static class Producer {
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "ru.yandex.practicum.serialization.AvroSerializer";
    }

    @Data
    public static class Consumer {
        private Duration pollTimeout = Duration.ofMillis(1000);
        private String groupId = "aggregator-group";
        private String clientId = "aggregator-consumer";
        private Integer maxPollRecords = 100;
        private Integer fetchMaxBytes = 3072000;
        private Integer maxPartitionFetchBytes = 307200;
        private Boolean enableAutoCommit = false;
        private String autoOffsetReset = "earliest";
        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
        private String valueDeserializer = "ru.yandex.practicum.serialization.SensorEventDeserializer";
        private List<String> topics = List.of();
    }
}