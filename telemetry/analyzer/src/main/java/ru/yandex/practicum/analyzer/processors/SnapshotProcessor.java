package ru.yandex.practicum.analyzer.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.KafkaConfiguration;
import ru.yandex.practicum.analyzer.services.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotProcessor {

    private final KafkaConfiguration kafkaConfig;
    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final SnapshotService snapshotService;

    public void start() {
        log.info("=== SnapshotProcessor STARTED ===");
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        List<String> topics = List.of(kafkaConfig.getSnapshotsTopic());
        consumer.subscribe(topics);

        try {
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(kafkaConfig.getPollTimeout());

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    // Сразу синхронно коммитим offset, по ТЗ "Повторная обработка снапшотов, напротив, крайне нежелательна"
                    commitOffset(record);

                    processRecord(record);
                }
            }
        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            log.info("Закрываем консьюмер");
            consumer.close();
        }
    }

    private void commitOffset(ConsumerRecord<String, SensorsSnapshotAvro> record) {
        try {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());

            // синхронный коммит блочит поток, гарантировано сначала коммит, потом обработка
            consumer.commitSync(Map.of(partition, new OffsetAndMetadata(record.offset() + 1)));
            log.debug("Offset закоммичен: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            log.error("Ошибка при коммите offset {} для снапшота", record.offset(), e);
            throw new RuntimeException("Не удалось закоммитить offset", e);
        }
    }

    private void processRecord(ConsumerRecord<String, SensorsSnapshotAvro> record) {
        log.debug("Обработка снапшота: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());

        SensorsSnapshotAvro value = record.value();
        if (value == null) {
            log.warn("Получено пустое сообщение, offset уже закоммичен");
            return;
        }

        try {
            snapshotService.handleSnapshot(value);
            log.debug("Снапшот успешно обработан: offset={}", record.offset());
        } catch (Exception e) {
            log.error("Ошибка обработки снапшота offset={}, offset уже закоммичен!", record.offset(), e);
        }
    }
}