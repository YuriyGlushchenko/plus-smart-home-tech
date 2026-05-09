package ru.yandex.practicum.analyzer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProps kafkaProps;

    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer() {
        KafkaConsumer<String, SensorsSnapshotAvro> consumer = new KafkaConsumer<>(
                kafkaProps.getConsumer().getSnapshot().getProperties()
        );
        consumer.subscribe(List.of(kafkaProps.getConsumer().getSnapshot().getTopic()));
        return consumer;
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer() {
        KafkaConsumer<String, HubEventAvro> consumer = new KafkaConsumer<>(
                kafkaProps.getConsumer().getHubEvent().getProperties()
        );
        consumer.subscribe(List.of(kafkaProps.getConsumer().getHubEvent().getTopic()));
        return consumer;
    }
}