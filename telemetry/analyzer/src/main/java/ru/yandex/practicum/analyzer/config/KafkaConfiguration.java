package ru.yandex.practicum.analyzer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.analyzer.config.KafkaProperties;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProps;

    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer() {
        Properties config = createBaseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProps.getConsumer().getSnapshot().getGroupId());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProps.getConsumer().getSnapshot().getClientId());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaProps.getConsumer().getSnapshot().getValueDeserializer());

        KafkaConsumer<String, SensorsSnapshotAvro> consumer = new KafkaConsumer<>(config);
        consumer.subscribe(List.of(kafkaProps.getTopics().getSnapshots()));

        return consumer;
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer() {
        Properties config = createBaseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProps.getConsumer().getHubEvent().getGroupId());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaProps.getConsumer().getHubEvent().getClientId());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaProps.getConsumer().getHubEvent().getValueDeserializer());

        return new KafkaConsumer<>(config);
    }

    private Properties createBaseConsumerConfig() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProps.getBootstrapServer());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProps.getConsumer().getKeyDeserializer());
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProps.getConsumer().getMaxPollRecords());
        config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, kafkaProps.getConsumer().getFetchMaxBytes());
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, kafkaProps.getConsumer().getMaxPartitionFetchBytes());
        return config;
    }
}