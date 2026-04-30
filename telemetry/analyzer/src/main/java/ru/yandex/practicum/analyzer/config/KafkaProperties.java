package ru.yandex.practicum.analyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private String bootstrapServer = "localhost:9092";
    private Topics topics = new Topics();
    private int offsetCommitCount = 10;
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();

    @Data
    public static class Topics {
        private String snapshots = "telemetry.snapshots.v1";
        private String hubs = "telemetry.hubs.v1";
    }

    @Data
    public static class Producer {
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "ru.yandex.practicum.serialization.AvroSerializer";
    }

    @Data
    public static class Consumer {
        private Duration pollTimeout = Duration.ofMillis(1000);
        private Integer maxPollRecords = 100;
        private Integer fetchMaxBytes = 3072000;
        private Integer maxPartitionFetchBytes = 307200;
        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";

        private Snapshot snapshot = new Snapshot();
        private HubEvent hubEvent = new HubEvent();

        @Data
        public static class Snapshot {
            private String groupId = "analyzer-snapshot-group";
            private String clientId = "analyzer-snapshot-consumer";
            private String valueDeserializer = "ru.yandex.practicum.serialization.SensorsSnapshotDeserializer";
        }

        @Data
        public static class HubEvent {
            private String groupId = "analyzer-hub-group";
            private String clientId = "analyzer-hub-consumer";
            private String valueDeserializer = "ru.yandex.practicum.serialization.HubEventDeserializer";
        }
    }
}