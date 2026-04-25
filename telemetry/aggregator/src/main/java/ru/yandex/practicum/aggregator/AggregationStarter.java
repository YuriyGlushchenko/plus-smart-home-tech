package ru.yandex.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс AggregationStarter, ответственный за запуск агрегации данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConfiguration kafkaConfig;
    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaConsumer<String, SpecificRecordBase> consumer;
    private final List<String> topics = List.of(kafkaConfig.getSensorsTopic());
    private final Map<String, SensorsSnapshotAvro> snapShots = new HashMap<>();

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();

    /**
     * Метод для начала процесса агрегации данных.
     * Подписывается на топики для получения событий от датчиков,
     * формирует снимок их состояния и записывает в кафку.
     */
    public void start() {

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(kafkaConfig.getPollTimeout());

                int count = 0;
                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    // обрабатываем очередную запись
                    handleRecord(record);
                    // фиксируем оффсеты обработанных записей, если нужно
                    manageOffsets(record, count, consumer);
                    count++;
                }

                // фиксируем максимальный оффсет обработанных записей
                consumer.commitAsync();


            }

        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {

            try {
                // Перед тем, как закрыть продюсер и консьюмер, нужно убедиться,
                // что все сообщения, лежащие в буффере, отправлены и
                // все оффсеты обработанных сообщений зафиксированы

                // здесь нужно вызвать метод продюсера для сброса данных в буффере
                // здесь нужно вызвать метод консьюмера для фиксации смещений
                producer.flush();

                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                }
            } catch (Exception e) {
                log.error("Ошибка flush/commit при закрытии продюсера", e);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<String, SpecificRecordBase> record, int count, KafkaConsumer<String, SpecificRecordBase> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(ConsumerRecord<String, SpecificRecordBase> record) {
        log.debug("топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                record.topic(), record.partition(), record.offset(), record.value());

        SpecificRecordBase value = record.value();

        if (value == null) {
            log.warn("Получено пустое сообщение");
            return;
        }

        if (!(value instanceof SensorEventAvro sensorEvent)) {
            log.warn("Сообщение не относится к событиям сенсоров: {}", value.getClass().getName());
            return;
        }

        Optional<SensorsSnapshotAvro> snapshotOpt = updateState(sensorEvent);

        snapshotOpt.ifPresent(this::sendSnapshotToBroker);
    }

    Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        SensorsSnapshotAvro snapshot = snapShots.get(event.getHubId());
        if (snapshot == null) {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(event.getHubId())
                    .setTimestamp(event.getTimestamp())
                    .setSensorsState(new HashMap<>())
                    .build();
        }

        if (snapshot.getSensorsState().containsKey(event.getId())) {
            SensorStateAvro oldState = snapshot.getSensorsState().get(event.getId());
            if (oldState.getTimestamp().isAfter(event.getTimestamp()) || oldState.getData().equals(event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro newSensorState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        snapshot.setTimestamp(event.getTimestamp());
        snapshot.getSensorsState().put(event.getId(), newSensorState);

        snapShots.put(event.getHubId(), snapshot);

        return Optional.of(snapshot);
    }

    private void sendSnapshotToBroker(SensorsSnapshotAvro snapshot) {
        if (snapshot == null) {
            return;
        }

        String key = snapshot.getHubId();
        long timestamp = snapshot.getTimestamp().toEpochMilli();

        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaConfig.getSnapshotsTopic(),
                null, // опредляем партицию на основе ключа
                timestamp,
                key,
                snapshot
        );


        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка при отправке snapshot в брокер", exception);
            } else {
                log.info("Snapshot успешно отправлен: hubId={}, offset={}", snapshot.getHubId(), metadata.offset());
            }
        });

    }

}
