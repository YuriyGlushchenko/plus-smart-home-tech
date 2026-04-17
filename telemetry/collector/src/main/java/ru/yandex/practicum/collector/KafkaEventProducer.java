package ru.yandex.practicum.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaConfiguration kafkaConfig;

    public CompletableFuture<RecordMetadata> sendSensorEvent(SensorEventAvro avroEvent) {
        String key = avroEvent.getHubId();
        long timestamp = avroEvent.getTimestamp().toEpochMilli();

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaConfig.getSensorsTopic(),
                null, // опредляем партицию на основе ключа
                timestamp, // timestamp самого события
                key,
                avroEvent
        );

        return sendToBroker(record);
    }

    public CompletableFuture<RecordMetadata> sendHubEvent(HubEventAvro avroEvent) {
        String key = avroEvent.getHubId();
        long timestamp = avroEvent.getTimestamp().toEpochMilli();

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaConfig.getHubsTopic(),
                null,
                timestamp,
                key,
                avroEvent
        );

        return sendToBroker(record);
    }

    private CompletableFuture<RecordMetadata> sendToBroker(ProducerRecord<String, SpecificRecordBase> record) {

        // CompletableFuture - аналог Promise из JS для работы с асинхронным send. Удобно использовать, не блокирует поток.
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                // когда send вернет metadata, резолвим созданный CompletableFuture и кладем в него эту metadata
                future.complete(metadata);

            } else {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

}