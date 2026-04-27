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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotProcessor {

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new ConcurrentHashMap<>();
    private final KafkaConfiguration kafkaConfig;
    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final SnapshotService snapshotService;


    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        List<String> topics = List.of(kafkaConfig.getSnapshotsTopic());

        try {
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(kafkaConfig.getPollTimeout());

                int count = 0;
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
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
                if (!currentOffsets.isEmpty()) {
                    consumer.commitSync(currentOffsets);
                }
            } catch (Exception e) {
                log.error("Ошибка flush/commit при закрытии продюсера", e);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<String, SensorsSnapshotAvro> record, int count, KafkaConsumer<String, SensorsSnapshotAvro> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % kafkaConfig.getOffsetCommitCount() == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(ConsumerRecord<String, SensorsSnapshotAvro> record) {
        log.debug("топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                record.topic(), record.partition(), record.offset(), record.value());

        SensorsSnapshotAvro value = record.value();

        if (value == null) {
            log.warn("Получено пустое сообщение");
            return;
        }

        snapshotService.handleSnapshot(value);
    }

}
